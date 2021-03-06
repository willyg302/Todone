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
	http.HandleFunc("/logout", handleLogout)
	http.HandleFunc("/get", handleGet)
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
// AUTHENTICATION & LOGOUT
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

func handleLogout(w http.ResponseWriter, r *http.Request) {
	if r.Method != "POST" {
		http.Error(w, "POST requests only", http.StatusMethodNotAllowed)
		return
	}
	cookie := &http.Cookie{
		Name: "todone",
		Value: "",
		Path: "/",
		MaxAge: -1,
		HttpOnly: true,
	}
	http.SetCookie(w, cookie)
}


////////////////////////////////////////
// TODO
////////////////////////////////////////

// A todo has the following properties:
//   - Content: a string, describing what the todo is about
//   - Interval [Start - End]: when the todo is "active"
//   - Completed: true if the todo has been marked as done

// A todo that is not completed, but whose End is in the past, is late.
// When the daily cron job runs, the todo's state is considered according
// to the following rules:
//   - If a todo is due THAT DAY, then the day is red if the todo is not
//     completed, otherwise it MAY be green
//   - Any late todos automatically make the day red

// The following todos show up in the list:
//   - Todos whose Interval overlaps with the selected interval
//   - Late todos (show up as red)


type Todo struct {
	Key string `datastore:",noindex"`
	Content string `datastore:",noindex"`
	Start time.Time
	End time.Time
	Completed bool
}

// Puts the todo, given its key, into the datastore
func (todo *Todo) put(c appengine.Context, key *datastore.Key) error {
	_, err := datastore.Put(c, key, todo)
	return err
}

// Inserts a new todo into the datastore
func (todo *Todo) create(c appengine.Context) error {
	skey, key, err := NewKey(c, "Todo")
	if err != nil {
		return err
	}
	todo.Key = skey
	todo.Completed = false
	return todo.put(c, key)
}

// Gets the todo from the datastore
func (todo *Todo) read(c appengine.Context) (*datastore.Key, *Todo, error) {
	key, err := datastore.DecodeKey(todo.Key)
	if err != nil {
		return nil, nil, err
	}
	if err = datastore.Get(c, key, todo); err != nil {
		return nil, nil, err
	}
	return key, todo, nil
}

func (todo *Todo) updateContent(c appengine.Context) error {
	key, toUpdate, err := todo.read(c)
	if err != nil {
		return err
	}
	toUpdate.Content = todo.Content
	return toUpdate.put(c, key)
}

func (todo *Todo) updateStart(c appengine.Context) error {
	key, toUpdate, err := todo.read(c)
	if err != nil {
		return err
	}
	toUpdate.Start = todo.Start
	return toUpdate.put(c, key)
}

func (todo *Todo) updateEnd(c appengine.Context) error {
	key, toUpdate, err := todo.read(c)
	if err != nil {
		return err
	}
	toUpdate.End = todo.End
	return toUpdate.put(c, key)
}

// Deletes the given todo
func (todo *Todo) delete(c appengine.Context) error {
	key, err := datastore.DecodeKey(todo.Key)
	if err != nil {
		return err
	}
	return datastore.Delete(c, key)
}

func handleTodo(c appengine.Context, w http.ResponseWriter, r *http.Request, verb string, payload string) {
	// Get the todo from the payload
	todo := &Todo{}
	if err := ReadJSON(payload, todo); err != nil {
		WriteResponse(c, w, r, false, err)
		return
	}
	// Handle the verb
	var err error
	success := true
	switch verb {
	case "create":
		err = todo.create(c)
	case "updateContent":
		err = todo.updateContent(c)
	case "updateStart":
		err = todo.updateStart(c)
	case "updateEnd":
		err = todo.updateEnd(c)
	case "delete":
		err = todo.delete(c)
	}
	if err != nil {
		success = false
	}
	WriteResponse(c, w, r, success, err)
}


////////////////////////////////////////
// DAY
////////////////////////////////////////

type Day struct {
	Key string `datastore:",noindex"`
	Date time.Time
	Status string
}


////////////////////////////////////////
// ACHIEVEMENT
////////////////////////////////////////

type Achievement struct {
	Key string `datastore:",noindex"`
	Name string
	Description string
	Awarded bool
}

// @TODO
// Get all todos intersecting date range

// Modify Day status (only to set to vacation, all other is determined server-side)



////////////////////////////////////////
// API ROOT
////////////////////////////////////////

// All data is mirrored by the global ClojureScript atom client-side.
// Thus, responses are simple a JSON object with the following keys:
//   - success (bool): true if everything is good server-side
//   - error: any error that occurred if success = false
// If actions return true, then patches can safely be applied client-side

// In some cases of extreme error (e.g. not using POST), this may instead
// return an HTTP error instead of a proper JSON object.

// Normally it'd be a good idea to do some sort of REST API, but in Go
// that's more trouble than it's worth. So instead we have a single
// endpoint that manages POST via JSON. Actions are encoded in the JSON.

func handleAPI(w http.ResponseWriter, r *http.Request) {
	if r.Method != "POST" {
		http.Error(w, "POST requests only", http.StatusMethodNotAllowed)
		return
	}
	c := appengine.NewContext(r)
	// First get the request
	req, err := ReadRequest(c, w, r)
	if err != nil {
		WriteResponse(c, w, r, false, err)
		return
	}
	// Now push it out to the proper function
	path := strings.Split(req.Action, "/")
	switch path[0] {
	case "todo":
		handleTodo(c, w, r, path[1], req.Payload)
	default:
		Log(c, r, "warning", "Unrecognized API endpoint hit: %s", path[1])
	}
}





////////////////////////////////////////
// DATASTORE GETS
////////////////////////////////////////

func handleGet(w http.ResponseWriter, r *http.Request) {
	if r.Method != "POST" {
		http.Error(w, "POST requests only", http.StatusMethodNotAllowed)
		return
	}
	//c := appengine.NewContext(r)
	//var days[] Day;
}


////////////////////////////////////////
// DAILY CRON JOB
////////////////////////////////////////

func handleCron(w http.ResponseWriter, r *http.Request) {
	// @TODO
}
