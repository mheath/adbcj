<html>
<head>
	<title>Database Results</title>
</head>
<body>
<h1>Results</h1>
<#list result as row>
	<p>
		${row.a} - ${row.b}
	</p>
</#list>
</body>
</html>