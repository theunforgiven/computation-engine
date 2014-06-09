define(['jquery', 'underscore', 'backbone', "hbars!view/computation-table"], function ($, _, Backbone, template) {
    var Computations = Backbone.Collection.extend({
        comparator: 'id',
        url: '/rest/products/'
    });

    var ComputationTable = Backbone.View.extend({
        tagName: "table",
        template: template,
        initialize: function () {
            this.model = new Computations();
            this.listenTo(this.model, "sync", this.render);
        },
        sortedModel: function () {
            return this.model.toJSON();
        },
        render: function () {
            var s = this.template({computations: this.sortedModel()});
            this.$el.html(s);
            return this;
        }
    });

    return {
        initialize: function () {
            var table = new ComputationTable();
            $("body").append(table.$el);
            table.model.fetch();
        }
    };
});