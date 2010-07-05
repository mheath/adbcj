<html>
<head>
	<title>Database Results</title>
</head>
<body>
<h1>Results</h1>
<p>
	MySQL count: ${count1}
</p>
<p>
	Postgresql count: ${count2}
</p>
<p>
	MySQL contacts:
	<ul>
		<#list contacts1 as contacts>
			<#list contacts as contact>
				${contact.name} - ${contact.phone}
			</#list>
		</#list>
	</ul>
</p>
<p>
	Postgresql contacts:
	<ul>
		<#list contacts2 as contacts>
			<#list contacts as contact>
				${contact.name} - ${contact.phone}
			</#list>
		</#list>
	</ul>
</p>

</body>
</html>