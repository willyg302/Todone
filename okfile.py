project = 'Todone'

def generate_app_yaml():
	'''Generate production app.yaml'''
	with open('dist/app.yaml', 'r+') as f:
		data = f.read().replace('{{ appname }}', raw_input('App name: '))
		f.seek(0)
		f.write(data)
		f.truncate()
	import getpass
	import hashlib
	with open('dist/todone.go', 'r+') as f:
		data = f.read().replace('{{ appkey }}', hashlib.sha256(getpass.getpass('App key: ')).hexdigest())
		f.seek(0)
		f.write(data)
		f.truncate()

def gae_server():
	'''Run local App Engine server'''
	ok.run('goapp serve dist/')

def gae_deploy():
	'''Deploy Todone to App Engine'''
	ok.run('goapp deploy dist/')

def build_server():
	'''Build only server-side, skipping Leiningen and CSS'''
	ok.node('gulp copy-assets', module=True).run(generate_app_yaml)

def build_css():
	'''Build only CSS'''
	ok.node('gulp compile-css', module=True)

def build():
	'''Build Todone'''
	ok.node('gulp', module=True).lein('cljsbuild once todone').run(generate_app_yaml)

def install():
	ok.npm('install').bower('install', root='app')

def default():
	ok.run(build)
