package todone

import (
	"testing"
	"appengine/aetest"
)

func TestHello(t *testing.T) {
	c, err := aetest.NewContext(nil)
	if err != nil {
		t.Fatal(err)
	}
	defer c.Close()
}
