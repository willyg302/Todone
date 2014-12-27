var gulp       = require('gulp');
var less       = require('gulp-less');
var minifycss  = require('gulp-minify-css');

var del        = require('del');


var paths = {
	assets: [
		'./app/*.html',
		'./app/*.yaml',
		'./app/*.go',
		'./app/img/**/*.*'
	],
	app: './app',
	dist: './dist',
	css: './app/less/main.less'
};

gulp.task('clean', function(cb) {
	del(paths.dist, cb);
});

gulp.task('copy-assets', function() {
	return gulp.src(paths.assets, {base: paths.app})
		.pipe(gulp.dest(paths.dist));
});

gulp.task('compile-css', function() {
	return gulp.src(paths.css)
		.pipe(less())
		.pipe(minifycss())
		.pipe(gulp.dest(paths.dist + "/css"));
});

gulp.task('default', ['clean'], function() {
	gulp.start('copy-assets', 'compile-css');
});
