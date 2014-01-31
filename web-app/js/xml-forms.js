/*
*
* Grails xml-forms plug-in
*
* Copyright 2013 Open-T B.V., and individual contributors as indicated
* by the @author tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Affero General Public License
* version 3 published by the Free Software Foundation.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Affero General Public License for more details.

* You should have received a copy of the GNU Affero General Public License
* along with this program.  If not, see http://www.gnu.org/licenses
*/


if (!window.xmlforms) {
	window.xmlforms={
        currentForm : {
            submitted:false,
            form:null,
            dialog:null
        }
    };
}

/**
 * Regular expression validator
 * @param value The value to check
 * @param element element The element
 * @param expression The regular expression
 * @returns True if valid
 */
xmlforms.regexpValidator = function (value,element,expression) {
	var theRegExp=new RegExp(expression);

	//true if ok, false if nok
	return this.optional(element) || theRegExp.exec(value);
};

/**
 * Minimum inclusive validator
 * @param value
 * @param element
 * @param expression
 * @returns True if valid
 */
xmlforms.minExclValidator = function (value,element,expression) {
	var theRegExp=new RegExp(expression);
	//true if ok, false if nok
	return this.optional(element) || value > param;
};

/**
 * Maximum exclusive validator
 * @param value
 * @param element
 * @param expression
 * @returns True if valid
 */
xmlforms.maxExclValidator = function (value,element,expression) {
	var theRegExp=new RegExp(expression);
	//true if ok, false if nok
	return this.optional(element) || value < param;
};

/**
 * Evaluate dependencies
 * Evaluate dependencies of fields in the form
 * Hides the fields that have an unsatisfied dependency
 * @param element
 * @returns
 */
xmlforms.evaluateDependencies = function(element) {
	val=element.value;
	name=element.name;
	$("*[depends-on='"+name+"']").each( function () {
		dependencyType=$(this).attr("dependency-type");
		dependencyParameter=$(this).attr("dependency-parameter");

		var show=false;

		switch (dependencyType) {
		case "nonempty":
			show = val !== null && val.length>0;
			break;
		case "empty":
			show = val === null || val.length===0;
			break;
		case "true":
			show = val !== null && val.length>0;
			break;
		case "false":
			show = val === null || val.length===0 || val==="0" || val ==="false";
			break;
		case "gt":
			show = val !== null && val>dependencyParameter;
		break;
		case "lt":
			show = val !== null && val<dependencyParameter;
			break;
		case "eq":
			show = val !== null && val===dependencyParameter;
			break;
		case "ne":
			show = val !== null || val!==dependencyParameter;
			break;
		default:
			show = (val !== null) && (val.length>0);
			break;
		}


		if (!show) {
			$(this).hide("fast");
			if ($(this).attr("name")) {
				$(this).attr("name",$(this).attr("name").replace("update-","hidden-"));
			}

		} else {
			$(this).show("fast");
			if ($(this).attr("name")) {
				$(this).attr("name",$(this).attr("name").replace("hidden-","update-"));
			}
		}

	}
	);
};

/**
 * Reload handler
 * This is performed on a full page reload
 * @returns {undefined}
 */
xmlforms.reload = function reload() {
    $("span.help").tooltip({container:'body',placement:'right'});
    $("a.help").tooltip({container:'body',placement:'right'});
    $("li.menu-item a").tooltip({container:'body',placement:'right'});
	$(".dependency-source").each(function() {
  		xmlforms.evaluateDependencies(this);
	});
};

/**
 * SHow a modal dialog
 * @param id
 * @param controllerName
 * @param options
 * dialogname
 * submitname
 * refresh
 * submitform
 * nosubmit
 * domainclass
 * @param urlParams
 * @returns {Boolean}
 */
xmlforms.formDialog = function (id,controllerName, options ,urlParams) {
	var urlId=id+window.dialog.obj2ParamStr(urlParams);

	var dialogName = (options !== null && options["dialogname"] !== null) ? options["dialogname"] : "dialog";
	var submitName = (options !== null && options["submitname"] !== null) ? options["submitname"] : "submit"+dialogName;

	var refreshTableKey = (options !== null && options["refresh"] !== null) ? options["refresh"] : "NO_REFRESH";

	// if true, form submit will be used instead of AJAX
	var submitForm = (options !== null && options["submitform"] !== null) ? options["submitform"] : false;

	// if true, form will not be submitted at all
	var noSubmit = (options !== null && options["nosubmit"] !== null) ? options["nosubmit"] : false;

	var domainClass = (options !== null && options["domainclass"] !== null) ? options["domainclass"] : controllerName.capitalize();

	theUrl=window.dialog.baseUrl+'/'+controllerName+'/'+dialogName+'/'+urlId;

	xmlforms.dialogHTML = $.ajax({
		url: theUrl,
		async: false,
		cache: false
	}).error(function(event, jqXHR, ajaxSettings, thrownError) {
        if (event.status>=400 && event.status<500) {
            window.location.reload();
        }
	}).responseText;


	var formelements=$(xmlforms.dialogHTML).find('form.xml-form');

    var innerFormText=$(xmlforms.dialogHTML).find('div.body').html();
    if (innerFormText) {
        xmlforms.dialogHTML=innerFormText;
    }

	if (formelements.length===0) {
		window.location.reload();
	} else {
        var theWidth="960";
        try {
          theWidth=$(xmlforms.dialogHTML).css("width") && $(xmlforms.dialogHTML).css("width")!="0px" ? $(xmlforms.dialogHTML).css("width").replace("px","") : "960";
        } catch(err) {
        }

        // If there was a previous modal, remove it from the DOM.
        if (xmlforms.currentForm.dialog) {
            $(xmlforms.currentForm.dialog).remove();
        }
        xmlforms.dialogOpen=false;

        $("#page").append(xmlforms.dialogHTML);


        xmlforms.currentForm.dialog=$("#page div.modal");
        $(xmlforms.currentForm.dialog).modal({show:false,backdrop:'static'});


		xmlforms.currentForm.dialog.draggable({
			handle: ".modal-header"
		});
		//theDialog.resizable();

		$(xmlforms.currentForm.dialog[0]).css("margin-left","-"+theWidth/2+"px");
        //$(xmlforms.currentForm.dialog[0]).css("top","50px");

		var submitCallback=function(frm) {
            xmlforms.currentForm.submitted=true;
			xmlforms.currentForm.form = $(frm);
			var $target = xmlforms.currentForm.form.attr('data-target');

			var formdata=xmlforms.currentForm.form.serialize();

			$.ajax({
				type: xmlforms.currentForm.form.attr('method'),
				url: xmlforms.currentForm.form.attr('action'),
				data: formdata,
				dataType: "json",
				success: function(data, status) {
					var jsonResponse=data.result;
					$(".dialog-events").trigger("dialog-refresh",{dc:domainClass,id:id,jsonResponse:jsonResponse});
				 	$(".dialog-events").trigger("dialog-message",{message:jsonResponse.message,alertType:'success'});

				 	if(jsonResponse.success){
				 		xmlforms.currentForm.dialog.modal("hide");
				 		if (jsonResponse.nextDialog) {
				 			xmlforms.formDialog(jsonResponse.nextDialog.id,jsonResponse.nextDialog.controllerName,jsonResponse.nextDialog.options,jsonResponse.nextDialog.urlParams);
				 		}
				 	} else  {
				 		for (key in jsonResponse.errorFields) {
				 			var errorField=jsonResponse.errorFields[key];
				 			$("#"+errorField).parent().addClass("errors");
				 		}
                        var msg='<div id="alertmessage" class="alert alert-error"><button type="button" class="close" data-dismiss="alert">×</button><div>'+jsonResponse.message+'</div></div>';
				 		xmlforms.currentForm.dialog.find("div.errors").html(msg);
				 		xmlforms.currentForm.dialog.find("div.errors").show();

			 		}
				 	xmlforms.currentForm.dialog.modal("hide");
				},
                error: function(data, status) {
                    var responseText=eval('('+data.responseText+')');
                    var jsonResponse=responseText.result;
					$(".dialog-events").trigger("dialog-refresh",{dc:domainClass,id:id,jsonResponse:jsonResponse});
				 	$(".dialog-events").trigger("dialog-message",{message:jsonResponse.message,alertType:'error'});
				 		/*for (key in jsonResponse.errorFields) {
				 			var errorField=jsonResponse.errorFields[key];
				 			$("#"+errorField).parent().addClass("errors");
				 		}
                    */
                        var msg='<div id="alertmessage" class="alert alert-error"><button type="button" class="close" data-dismiss="alert">×</button><div>'+jsonResponse.message+'</div></div>';
				 		xmlforms.currentForm.dialog.find("div.errors").html(msg);
				 		xmlforms.currentForm.dialog.find("div.errors").show();

                }

			});
			//event.preventDefault();
		};

		xmlforms.currentForm.dialog.on('show', function (event) {
            if (!xmlforms.dialogOpen) {
                xmlforms.dialogOpen=true;

                $(this).trigger("dialog-open",{event:event,ui:null,'this':this,id:id,controllerName:controllerName});

                $(this).keyup(function(e) {
                    if (e.keyCode === 13 && e.target.nodeName!=="TEXTAREA") {
                        $(this).parents('.ui-dialog').first().find('.ui-button').first().click();
                    return false;
                }
                });


                $(this).find(".altselect").altselect();


                $(this).find(".help").tooltip({container:'body',placement:'right'});
                $(this).find(".help-tooltip").tooltip({container:'body'});

                // TODO use the validate submission callback, see http://jqueryvalidation.org/validate
                $(this).find('#form').validate({
                    submitHandler: function(form) {
                        submitCallback(form);
                    },
                     invalidHandler: function(event, validator) {
                        var errors = validator.numberOfInvalids();
                        $("a[href^='#tab']").parent().removeClass('alertTab');

                        if (errors) {
                            var message = errors === 1 ? '1 field has errors. It has been highlighted': errors + ' fields have errors. They have been highlighted';
                            var errorHTML='<div class="alert alert-error"><button type="button" class="close" data-dismiss="alert">&times;</button><strong>Error!</strong> '+message+'</div>';
                            $(xmlforms.currentForm.dialog).find("div.modal-body div.alert.alert-error").remove();
                            $(xmlforms.currentForm.dialog).find("div.modal-body").prepend(errorHTML);

                            for (i = 0; i < validator.invalidElements().length; i++)
                            {
                                var elementWithError = ($(xmlforms.currentForm.dialog).find(validator.invalidElements()[i]))[0];

                                var tab = $(elementWithError).closest($('div.tab-pane')).attr('id');
                                $("a[href='#" + tab + "']").parent().addClass('alertTab');
                            }
                        }
                     }
                });

				var theFieldData;
				$("input, select, textarea").focus( function() {
					theFieldData = $(this).val();
				});

				$("input, select, textarea").blur( function() {
					if(theFieldData !== $(this).val()) {
						markAsEditTab(this);
					}
				});

				var markAsEditTab = function(elem) {
					var tab = $(elem).closest($('div.tab-pane')).attr('id');

					if ($(xmlforms.currentForm.dialog).find("a[href='#" + tab + "'] sup").length < 1) {
						$(xmlforms.currentForm.dialog).find("a[href='#" + tab + "']").append("&nbsp;<sup>*</sup>");
					}
				};

                $(this).find("input[type!='hidden'],select,textarea").filter(":first").focus();
            }

		});

		var theForm=$(xmlforms.currentForm.dialog).find("form")[0];

		$(theForm).on('submit', function(event) {
			//submitCallback(theForm);
			xmlforms.currentForm.form = $(this);
			var $target = xmlforms.currentForm.form.attr('data-target');

			var formdata=xmlforms.currentForm.form.serialize();

			//alert('test');
			event.preventDefault();
		});

		$(xmlforms.currentForm.dialog).on('hide',function(event) {
 			$(this).trigger("dialog-close",{event:event,ui:null,'this':this,currentForm:xmlforms.currentForm});
		});
		xmlforms.currentForm.dialog.modal('show');
		xmlforms.resizeDialog();
	}

};

//This is performed on a formDialog show event or on window resizing
xmlforms.resizeDialog = function () {
	var windowHeight = $(window).height();

	$(xmlforms.currentForm.dialog).find('.modal-body').css({
		maxHeight: (windowHeight * 0.9) - 136
	});

	$(xmlforms.currentForm.dialog).find('.modal-body .tab-content').css({
		maxHeight: (windowHeight * 0.9) - 136 - ($(xmlforms.currentForm.dialog).find('.modal-body fieldset').height() - $(xmlforms.currentForm.dialog).find('.modal-body .tab-content').height())
	});
}



/*
 * jQuery processsing which takes place after page reload
 */

jQuery(function(){
  	xmlforms.reload();

  	// This copies the checkbox value into the corresponding (hidden) update field so we have a value in the post even if the checkbox value=false
  	$(document).on ("click" , 'input[type="checkbox"]',function(e) {
  		var updateName=this.name.replace("entry-","update-").replace(/\./g, "\\.").replace(/\[/g, "\\[").replace(/\]/g, "\\]");
  		$('input[name="'+updateName+'"]').val($(this).attr("checked")==='checked');
  	});

	// add method to validate plugin
	$.validator.addMethod("regexp",xmlforms.regexpValidator,"Value does not match regexp");
	$.validator.addMethod("minexcl",xmlforms.minExclValidator,"Value too small");
	$.validator.addMethod("maxexcl",xmlforms.maxExclValidator,"Value too large");

	$(document).on ("blur" , '.dependency-source',function(e) {
			xmlforms.evaluateDependencies(e.target);
		}
	);

	// Perform any jQuery initialization that needs to be redone after a form reload
	//onFormReload()

	// Add a node to a form
	$(document).on ("click" , '.action-insert',function(e) {
		var parentid=$(this).attr("parentid");
		var formData=$("#form").serialize();

		 $.ajax({
 			url: this.href,
 			async: true,
 			data: formData,
 			type:'POST',
 			success: function(data) {
 				var newHTML = $(data).find("#"+parentid);
 				$("#"+parentid).html(newHTML);
 				$(".dialog-events").trigger("dialog-refresh",{});
 			}
		 });

		return false;
	});


	// Delete a node from a form
	$(document).on ("click" , '.action-delete',function(e) {
    	var parentid=$(this).attr("parentid");
		var formData=$("#form").serialize();

		$.ajax({
 			url: this.href,
 			async: true,
 			data: formData,
 			type:'POST',
 			success: function(data) {
 				var newHTML = $(data).find("#"+parentid);
 				$("#"+parentid).html(newHTML);
 				$(".dialog-events").trigger("dialog-refresh",{});
 			}
		 });
		return false;
  });

  // Set the outcome of a submit button to the outcome hidden field.
  $(document).on ("click" , '.outcome-submit',function(event) {
    var submitButton=$(event.currentTarget);
    var outcome=submitButton.attr("outcome");
    var outcomeFieldId=submitButton.attr("outcome-id");
    $("#"+outcomeFieldId).val(outcome);
  });
});


jQuery(function() {
	$("body").on("dialog-refresh",xmlforms.reload);
	$(window).resize(xmlforms.resizeDialog);
});

