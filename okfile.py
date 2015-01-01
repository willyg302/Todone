project = 'Todone'


# BUILD TASKS

def _fill_template(filename, template, value):
	with open(filename, 'r+') as f:
		data = f.read().replace("{{ " + template + " }}", value)
		f.seek(0)
		f.write(data)
		f.truncate()

def fill_templates():
	_fill_template('dist/app.yaml', 'appname', raw_input('App name: '))
	import getpass
	import hashlib
	_fill_template('dist/todone.go', 'appkey', hashlib.sha256(getpass.getpass('App key: ')).hexdigest())

def build_server():
	'''Build only server-side, skipping Leiningen and CSS'''
	ok.node('gulp copy-assets', module=True).run(fill_templates)

def build_css():
	'''Build only CSS'''
	ok.node('gulp compile-css', module=True)

def build_client():
	'''Build only client-side'''
	ok.run(build_css).lein('cljsbuild once todone')

def build():
	'''Build Todone'''
	ok.node('gulp', module=True).lein('cljsbuild once todone').run(fill_templates)


# DEPLOYMENT TASKS

def gae_server():
	'''Run local App Engine server'''
	ok.goapp('serve dist/')

def gae_deploy():
	'''Deploy Todone to App Engine'''
	ok.goapp('deploy dist/')


# BASIC TASKS

def test():
	ok.goapp('test ./app')

def install():
	ok.npm('install').bower('install', root='app')

def default():
	# @NOTE: Very expensive for a full rebuild. Avoid doing this except for
	#        a clean slate; defer to specific build tasks above instead.
	ok.run([test, build])
