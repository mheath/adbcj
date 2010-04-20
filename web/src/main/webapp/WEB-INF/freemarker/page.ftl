<html>
<head>
	<title>Database Results</title>
</head>
<body>
<#list results as resultSet>
	<#list resultSet as result>
		<p>${result.a}</p>
		<p>${result.b}</p>
		<p>${result.c}</p>
	</#list>
	<hr/>
</#list>
</body>
</html>