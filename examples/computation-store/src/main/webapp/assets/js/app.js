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
        events: {
            "click .computation-row .save": "saveRow",
            "click .computation-row .update": "updateRow",
            "click .computation-row .delete": "deleteRow"
        },
        saveRow: function(ev) {
            var tableRow = $(ev.target).parents('tr');
            var me = this;
            var rowName = $(tableRow).find('input[name="name"]').val()
            var rowCost = parseFloat($(tableRow).find('input[name="cost"]').val())
            this.model.create({name: rowName, cost: rowCost})
        },
        updateRow: function(ev) {
            var rowId = $(ev.target).data("row-id");
            var tableRow = $(ev.target).parents('tr');
            var row = this.model.findWhere({id: rowId});
            var me = this;
            row.set("name", $(tableRow).find('input[name="name"]').val());
            row.set("cost", parseFloat($(tableRow).find('input[name="cost"]').val()));
            row.save({
                success: function(model, response) {
                    me.render();
                },
                error: function(model, response) {
                    alert("Error deleting product: " + response.statusText);
                }
            });
        },
        deleteRow: function(ev) {
            var rowId = $(ev.target).data("row-id");
            var tableRow = $(ev.target).parents('tr');
            var row = this.model.findWhere({id: rowId});
            var me = this;
            row.destroy({
                success: function(model, response) {
                    me.render();
                    alert("deleted");
                },
                error: function(model, response) {
                    alert("Error deleting product: " + response.statusText);
                }
            });
        },
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