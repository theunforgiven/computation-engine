define(['jquery', 'underscore', 'backbone'], function ($, _, Backbone) {
    return {
        initialize: function () {
            var list = '<% _.forEach(computations, function(c) { %><tr><td><%- c.get("id") %></td><td><%- c.get("name") %></td><td><%- c.get("cost") %></td></tr><% }); %>';
            var t = _.template(list);

            var Computation = Backbone.Model.extend({
            });
            var Computations = Backbone.Collection.extend({
                model: Computation,
                url: '/rest/products/'
            });


            new Computations().fetch({
                success: function (computations) {
                    var sortedComputations = computations.sortBy(_.property("id"));
                    var s = t({computations: sortedComputations});
                    $("#computations").html(s);
                }
            });
        }
    };
});