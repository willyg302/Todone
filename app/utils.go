package todone

import(
	"encoding/json"
	"fmt"
	"html/template"
	"net/http"

	"appengine"
	"appengine/datastore"
)


// Logs the given `message` to App Engine with the given `priority`
// Accepted priority values: debug, info, warning, error (default), critical
func Log(c appengine.Context, r *http.Request, priority string, message string, params ...interface{}) {
	message = fmt.Sprintf("[%s] [%s] [%s]: %s", r.RemoteAddr, r.Method, r.URL, message)
	switch priority {
	case "debug":
		c.Debugf(message, params...)
	case "info":
		c.Infof(message, params...)
	case "warning":
		c.Warningf(message, params...)
	case "error":
		c.Errorf(message, params...)
	case "critical":
		c.Criticalf(message, params...)
	default:
		c.Errorf(message, params...)
	}
}

// Serves the given template `t` from the given `bundle` of templates
func serveTemplate(w http.ResponseWriter, r *http.Request, bundle *template.Template, t string) {
	c := appengine.NewContext(r)
	if err := bundle.ExecuteTemplate(w, t, nil); err != nil {
		Log(c, r, "error", "Failed to serve template (%s): %v", t, err)
	}
}

// Allocates a new key for the given `kind`, also returning its string encoding
func NewKey(c appengine.Context, kind string) (string, *datastore.Key, error) {
	id, _, err := datastore.AllocateIDs(c, kind, nil, 1)
	if err != nil {
		return "", nil, err
	}
	key := datastore.NewKey(c, kind, "", id, nil)
	return key.Encode(), key, nil
}

func ReadJSON(data string, v interface{}) error {
	return json.Unmarshal([]byte(data), v)
}


////////////////////////////////////////
// REQUEST - RESPONSE HANDLING
////////////////////////////////////////

type Request struct {
	Action string
	Payload string
}

func ReadRequest(c appengine.Context, w http.ResponseWriter, r *http.Request) (*Request, error) {
	req := &Request{}
	if err := json.NewDecoder(r.Body).Decode(req); err != nil {
		Log(c, r, "error", "Failed to read request: %v", err)
		return nil, err
	}
	return req, nil
}

type Response struct {
	Success bool
	Error error
}

func WriteResponse(c appengine.Context, w http.ResponseWriter, r *http.Request, success bool, e error) {
	resp := &Response{
		Success: success,
		Error: e,
	}
	data, err := json.Marshal(resp)
	if err != nil {
		// At this point if an error occurs we just say screw it
		http.Error(w, "The server has a dumb", http.StatusInternalServerError)
		return
	}
	w.Header().Set("Content-Type", "text/json; charset=utf-8")
	_, err = w.Write(data)
	if err != nil {
		Log(c, r, "error", "Failed to write response (%v): %v", string(data), err)
	}
}
