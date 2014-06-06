require.config({
  paths: {
    text: 'lib/text',
    jquery: 'lib/jquery',
    underscore: 'lib/lodash',
    backbone: 'lib/backbone'
  }
});

require(['app'], function(App){
    App.initialize();
});