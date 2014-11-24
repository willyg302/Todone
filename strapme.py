project = 'Todone'

def generate_app_yaml():
	'''Generate production app.yaml'''
	with open('dist/app.yaml', 'r+') as f:
		data = f.read() \
			.replace('{{ appname }}', raw_input('App name: '))
		f.seek(0)
		f.write(data)
		f.truncate()

def gae_server():
	'''Run local App Engine server'''
	strap.run('goapp serve dist/')

def gae_deploy():
	'''Deploy Todone to App Engine'''
	strap.run('goapp deploy dist/')

def build():
	'''Build Todone'''
	strap.node('gulp', module=True).run('lein cljsbuild once todone').run(generate_app_yaml)

def install():
	strap.npm('install').bower('install', root='app')

def default():
	strap.run(build)
