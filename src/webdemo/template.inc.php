<!DOCTYPE html>
<html>
<head>

    <!-- Bootstrap -->
    <link href="js/bootstrap/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="js/treeview/jquery.treeview.css" />
    <link href="style.css" rel="stylesheet">

    <!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
    <!--[if lt IE 9]>
      <script src="https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js"></script>
      <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
    <![endif]-->

</head>
<body>
	<div class="page-header">
		<div class='pull-right logos'>
			<a href="http://dkm.fbk.eu/"><img src="images/fbkdkm.png"/></a>&nbsp;&nbsp;
			<a href="http://www.newsreader-project.eu/"><img src="images/newsreader.png"/></a>
		</div>
		<h1>ESO reasoner <small>Newsreader EU project</small></h1>
	</div>

	<div class='container-fluid'>
		<div class="row">
			<div class="col-md-3">
				<h2>Select file</h2>
				<form enctype="multipart/form-data" method="POST" id="fileForm">
					<div class="form-group">
						<label for="exampleInputFile">Input file</label>
						<input type="file" name="userfile" id="userfile">
						<p class="help-block">File must be in TriG format.</p>
					</div>
					<div class="checkbox">
						<label>
						<input name="example" type="checkbox"> Use an example file (upload no needed)
						</label>
					</div>
					<div class="checkbox">
						<label>
						<input name="include" type="checkbox"> Include events without situations
						</label>
					</div>
					<div class="checkbox">
						<label>
						<input name="details" type="checkbox"> Show complete details
						</label>
					</div>
					<input class='btn btn-primary' type="submit" value="Send" id="submitButton" data-loading-text="Loading...">
				</form>
			</div>
			<div class="col-md-9">
				<?php echo $Text; ?>
			</div>
		</div>
	</div>

	<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js"></script>
	<script type="text/javascript" src="js/bootstrap/js/bootstrap.min.js"></script>
    <script src="js/treeview/lib/jquery.cookie.js" type="text/javascript"></script>
    <script src="js/treeview/jquery.treeview.js" type="text/javascript"></script>
    <script type="text/javascript" src="js/custom.js"></script>

</body>
</html>