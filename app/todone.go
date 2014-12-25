package todone

import(
	"html/template"
	"net/http"
	"strings"
	"time"

	"appengine"
	"appengine/datastore"
)


func init() {
	http.HandleFunc("/", handleRoot)
	http.HandleFunc("/auth", handleAuth)
	http.HandleFunc("/todone", handleAPI)
	http.HandleFunc("/cron", handleCron)
}

var templates = template.Must(template.ParseGlob("*.html"))


////////////////////////////////////////
// ROOT HANDLER
////////////////////////////////////////

func serveRoot(w http.ResponseWriter, r *http.Request) {
	serveTemplate(w, r, templates, "index.html")
}

func handleRoot(w http.ResponseWriter, r *http.Request) {
	if r.Method != "GET" {
		http.Error(w, "GET requests only", http.StatusMethodNotAllowed)
		return
	}
	if r.URL.Path != "/" {
		http.NotFound(w, r)
		return
	}
	// Now authenticate
	cookie, err := r.Cookie("todone")
	if err != nil || cookie.Value != APP_KEY {
		http.Redirect(w, r, "/auth", http.StatusSeeOther)
		return
	}
	serveRoot(w, r)
}


////////////////////////////////////////
// AUTHENTICATION
////////////////////////////////////////

var APP_KEY = "{{ appkey }}"

func serveAuth(w http.ResponseWriter, r *http.Request) {
	serveTemplate(w, r, templates, "auth.html")
}

func validateAuth(w http.ResponseWriter, r *http.Request) {
	auth := r.FormValue("auth")
	if auth != APP_KEY {
		http.Redirect(w, r, "/auth", http.StatusSeeOther)
		return
	}
	cookie := &http.Cookie{
		Name: "todone",
		Value: APP_KEY,
		Path: "/",
		Expires: time.Now().Add(30 * 24 * time.Hour),
		HttpOnly: true,
	}
	http.SetCookie(w, cookie)
	http.Redirect(w, r, "/", http.StatusSeeOther)
}

func handleAuth(w http.ResponseWriter, r *http.Request) {
	if r.Method == "GET" {
		serveAuth(w, r)
	} else if r.Method == "POST" {
		validateAuth(w, r)
	} else {
		http.Error(w, "GET or POST requests only", http.StatusMethodNotAllowed)
	}
}


////////////////////////////////////////
// TODO
////////////////////////////////////////

type Todo struct {
	Key string `datastore:",noindex"`
	Content string `datastore:",noindex"`
	Start time.Time
	End time.Time
	Completed bool
	Pushed time.Time
}

// Puts a todo, given its key, into the datastore and returns it
func (todo *Todo) put(c appengine.Context, key *datastore.Key) *Todo {
	_, err := datastore.Put(c, key, todo)
	if err != nil {
		return nil
	}
	return todo
}

// Inserts a new todo into the datastore, returning it
func (todo *Todo) create(c appengine.Context) *Todo {
	skey, key, success := NewKey(c, "Todo")
	if !success {
		return nil
	}
	todo.Key = skey
	todo.Completed = false
	todo.Pushed = *new(time.Time)
	return todo.put(c, key)
}

// Gets the todo from the datastore
func (todo *Todo) read(c appengine.Context) (*datastore.Key, *Todo) {
	key, err := datastore.DecodeKey(todo.Key)
	if err != nil {
		return nil, nil
	}
	if err = datastore.Get(c, key, todo); err != nil {
		return nil, nil
	}
	return key, todo
}

// Updates the todo with `updates`, returning the updated version
func (todo *Todo) update(c appengine.Context) *Todo {
	key, toUpdate := (&Todo{
		Key: todo.Key,
	}).read(c)
	if todo.Content != "" {
		toUpdate.Content = todo.Content
	}
	var defaultTime time.Time
	if todo.Start != defaultTime {
		toUpdate.Start = todo.Start
	}
	if todo.End != defaultTime {
		toUpdate.End = todo.End
	}
	return toUpdate.put(c, key)
}

// Deletes the given todo
func (todo *Todo) delete(c appengine.Context) bool {
	key, err := datastore.DecodeKey(todo.Key)
	if err != nil {
		return false
	}
	if err := datastore.Delete(c, key); err != nil {
		return false
	}
	return true
}


////////////////////////////////////////
// DAY
////////////////////////////////////////


type Day struct {
	Key string `datastore:",noindex"`
	Date time.Time
	Status string
}

type Achievement struct {
	Key string `datastore:",noindex"`
	Name string
	Description string
	Awarded bool
}


// Get all todos intersecting date range

// Modify Day status (only to set to vacation, all other is determined server-side)




func handleTodo(c appengine.Context, w http.ResponseWriter, r *http.Request, verb string, payload string) {
	todo := &Todo{}
	if err := ReadJSON(payload, todo); err != nil {
		return
	}
	switch verb {
	case "create":
		todo = todo.create(c)
	case "update":
		todo = todo.update(c)
	case "delete":
		todo.delete(c)
	}
}


// Normally it'd be a good idea to do some sort of REST API, but in Go
// that's more trouble than it's worth. So instead we have a single
// endpoint that manages POST via JSON. Actions are encoded in the JSON.
func handleAPI(w http.ResponseWriter, r *http.Request) {
	c := appengine.NewContext(r)
	req, err := ReadRequest(c, w, r)
	if err != nil {
		return
	}
	path := strings.Split(req.Action, "/")
	switch path[0] {
	case "todo":
		handleTodo(c, w, r, path[1], req.Payload)
	}
}


////////////////////////////////////////
// DAILY CRON JOB
////////////////////////////////////////

func handleCron(w http.ResponseWriter, r *http.Request) {
	//
}
