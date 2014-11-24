var gulp       = require('gulp');
var less       = require('gulp-less');
var minifycss  = require('gulp-minify-css');
var uglify     = require('gulp-uglify');

var del        = require('del');


var paths = {
	assets: [
		'./app/index.html',
		'./app/app.yaml',
		'./app/todone.go'
	],
	app: './app',
	dist: './dist',
	css: './app/less/main.less',
	js: './app/js/app.js'
};

gulp.task('clean', function(cb) {
	del(paths.dist, cb);
});

gulp.task('copy-assets', function() {
	return gulp.src(paths.assets, {base: paths.app})
		.pipe(gulp.dest(paths.dist));
});

gulp.task('compile-css', function() {
	/*return gulp.src(paths.css)
		.pipe(less())
		.pipe(minifycss())
		.pipe(gulp.dest(paths.dist));*/
});

gulp.task('compile-js', function() {
	/*return browserify(paths.js)
		.transform(reactify)
		.bundle()
		.pipe(vinyl('main.js'))
		.pipe(buffer())
		.pipe(uglify())
		.pipe(gulp.dest(paths.dist));*/
});

gulp.task('default', ['clean'], function() {
	gulp.start('copy-assets', 'compile-css', 'compile-js');
});
