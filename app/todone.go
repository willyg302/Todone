package todone

import(
	"net/http"
)

func init() {
	http.HandleFunc("/todone", handler)
}

func handler(w http.ResponseWriter, r *http.Request) {
	//
}
