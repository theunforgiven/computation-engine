require.config({
  paths: {
    jquery: 'lib/jquery',
    underscore: 'lib/lodash',
    backbone: 'lib/backbone'
  }
});

require(['app'], function(App){
  // The "app" dependency is passed in as "App"
  App.initialize();
});