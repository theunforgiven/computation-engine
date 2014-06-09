require.config({
  paths: {
    text: 'lib/text',
    jquery: 'lib/jquery',
    underscore: 'lib/lodash',
    Handlebars: 'lib/handlebars',
    hbars: 'lib/hbars',
    backbone: 'lib/backbone'
  },
  shim: {
    Handlebars: {
        exports: 'Handlebars'
    }
  },
  hbars: {
    extension: '.hbs'
  }
});

require(['app'], function(App){
    $(function() {
        App.initialize($("body"));
    })
});