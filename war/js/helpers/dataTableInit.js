'use strict';


// The main namespace for our application
var rteam = (function(r, $) {
	r.init = function() {

	  r.activityTable = $("#gameActivity").dataTable({
	      "sDom": 'T<"clear">lfrtip',
	      "sScrollY": "100px",
	      "bPaginate": false,
	      "bFilter": false,
	      "bSort": false,
	      "bJQueryUI": false,
	      "bDestroy" : true,
	      "bServerSide": false,
	      "aaData": ["please wait, loading game activity ..."],
	      "aoColumnDefs": [
	        { "sWidth": "100%", "bSearchable": false, "bSortable": false, "sClass": "btip", "bVisible": true, "aTargets": [0] }
	       ],
	      "fnDrawCallback": function(){
	          console.log("fnDrawCallback entered");
	          r.initRows();
	          r.updateRows();
	      },
	    });

	    r.initRowDetailsCol();
	};


	r.initRows = function() {
	  console.log("initRows entered");
	};

	r.updateDB = function() {
	};

	r.updateRows = function() {
	  console.log("updateRows entered");
	};

	r.rowChanged = function() {
	};

	r.rowUpdated = function() {
	};

	r.formatRowDetails = function( oTable, nTr )
	{
	  console.log("***** formatRowDetails(): row number = " + nTr.epsRowNum);
	  var template = _.template($('#rowDetailsTemplate').html());
	  var content = template({rowNum: nTr.epsRowNum});
	  return content;
	};
	
	r.initRowDetailsCol = function() {
	    console.log("initRowDetailsCol() entered");
	};

  return r;
}(rteam || {}, jQuery));

