package todone

import (
	"testing"
	"appengine/aetest"
)

func TestUtils(t *testing.T) {
	c, err := aetest.NewContext(nil)
	if err != nil {
		t.Fatal(err)
	}
	defer c.Close()
}
