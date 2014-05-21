modules = {
	'xml-forms' {
		dependsOn 'dialog,dialog-dataTables,dialog-altselect,dialog-ckeditor,dialog-codemirror,bootstrap-css,bootstrap-tooltip,bootstrap-popover,bootstrap-modal'
		resource url:'js/xml-forms.js'
		resource url:'css/xml-forms.css'
		resource url:'js/jquery/jquery.validate.pack.js'
	}

	'xml-forms-datepicker' {
        dependsOn 'xml-forms,dialog-maskedinput'
		resource url:'js/jquery/jquery-ui-timepicker-addon.js'
		resource url:'js/xml-forms.datepicker.js'
	}

    'xml-forms-tabs' {
        dependsOn 'xml-forms'
		resource url:'js/xml-forms.tabs.js'
	}
}
