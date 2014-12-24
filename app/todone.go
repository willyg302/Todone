package todone

import(
	"html/template"
	"net/http"
	"time"

	"appengine"
	//"appengine/datastore"
)

type Todo struct {
	Content string
	Date    time.Time
}

func init() {
	http.HandleFunc("/", handleRoot)
	http.HandleFunc("/auth", handleAuth)
	http.HandleFunc("/todone", handleAPI)
}

var templates = template.Must(template.ParseGlob("*.html"))


////////////////////////////////////////
// GENERIC FUNCTIONS
////////////////////////////////////////

func serveTemplate(w http.ResponseWriter, r *http.Request, template string) {
	c := appengine.NewContext(r)
	if err := templates.ExecuteTemplate(w, template, nil); err != nil {
		c.Errorf("%v", err)
	}
}


////////////////////////////////////////
// ROOT HANDLER
////////////////////////////////////////

func serveRoot(w http.ResponseWriter, r *http.Request) {
	serveTemplate(w, r, "index.html")
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
	serveTemplate(w, r, "auth.html")
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
// TODONE API
////////////////////////////////////////

// Normally it'd be a good idea to do some sort of REST API, but in Go
// that's more trouble than it's worth. So instead we have a single
// endpoint that manages POST via JSON. Actions are encoded in the JSON.
func handleAPI(w http.ResponseWriter, r *http.Request) {
	//
}
