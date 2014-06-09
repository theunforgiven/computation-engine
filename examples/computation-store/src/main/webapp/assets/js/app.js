define(['jquery',
        'underscore',
        'backbone',
        'hbars!view/computation-table'],
function ($,
          _,
          Backbone,
          template) {
    var Computations = Backbone.Collection.extend({
        comparator: 'id',
        url: '/rest/products/'
    });

    var ExampleBaseView = Backbone.View.extend({
        serialized: function() {
            return this.model.toJSON();
        }
    });

    var ComputationTable = ExampleBaseView.extend({
        tagName: "table",
        template: template,
        initialize: function () {
            this.model = new Computations();
            this.listenTo(this.model, "sync", this.render);
        },
        render: function () {
            var s = this.template({computations: this.serialized()});
            this.$el.html(s);
            return this;
        }
    });

    return {
        initialize: function ($el) {
            var table = new ComputationTable();
            $el.append(table.$el);
            table.model.fetch();
        }
    };
});