var StatsExtension = {};

DataTableColumnHeaderUI.extendMenu(function(column, columnHeaderUI, menu) {
    var doStatsDialog = function(response) {
    	/* WIRING */
    	console.log("in doStatsDialog");
        var dialog = $(DOM.loadHTML("opentree", "scripts/stats-dialog.html"));
    
        var elmts = DOM.bind(dialog);
        elmts.dialogHeader.text("Opentree induced sub-tree from Google Refine data");

        //if (response["ottIds"]) { elmts.dialogIds.text(response["ottIds"]); }
        
        if (response["newick"]) { 
        	elmts.dialogNewick.text(response["newick"]);
        	console.log(response["newick"]);
        	console.log('set newickstr');        	
        }
        
        var level = DialogSystem.showDialog(dialog);

        elmts.okButton.click(function() {
            DialogSystem.dismissUntil(level - 1);
        });
        console.log('done doStatsDialog');
    };

    var prepStatsDialog = function() { 
        params = { "column_name": column.name };
        body = {};
        updateOptions = {};
        callbacks = { 
            "onDone": function(response) {
                doStatsDialog(response);
            }
        }
        console.log("in prepStatsDialog");
        /* WIRING */
        Refine.postProcess(
            "opentree",
            "subtree",
            params,
            body,
            updateOptions,
            callbacks
        );
    }

    MenuSystem.insertAfter(
        menu,
        [ "core/edit-column" ],
        {
            id: "opentree/subtree",
            label: "Opentree",
            click: prepStatsDialog 
        }
    );
});
