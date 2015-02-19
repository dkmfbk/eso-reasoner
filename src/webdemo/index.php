<?php

require "config.inc.php";

ob_start();

echo "<h2>Results</h2>";

if (isset($_REQUEST['example'])) {
	$in = "$F/sem.trig";
	$out = tempnam(sys_get_temp_dir(), "reasoning-out");
	$html = tempnam(sys_get_temp_dir(), "reasoning-html");
	$go = true;
}
elseif (isset($_FILES['userfile'])) {
	try {
		if ($_FILES['userfile']['error'] !== UPLOAD_ERR_OK) { 
			throw new UploadException($_FILES['userfile']['error']); 
		}

		$in = $_FILES['userfile']['tmp_name']."-in";
		$out = $_FILES['userfile']['tmp_name']."-out";
		$html = $_FILES['userfile']['tmp_name']."-html";

		move_uploaded_file($_FILES['userfile']['tmp_name'], $in);

		$go = true;
	}
	catch (Exception $e) {
		echo '<div class="alert alert-danger" role="alert">', $e->getMessage(), '</div>';
	}
}

if (isset($go) && $go) {
	$rdfp_command = sprintf($rdfp_command, ".trig:".$in, ".tql:".$out);
	// echo "<p>$rdfp_command</p>";
	shell_exec($rdfp_command);

	$html_command = sprintf($html_command, $out, $html);
	if (isset($_REQUEST['include']) && $_REQUEST['include']) {
		$html_command .= " -a";
	}
	if (isset($_REQUEST['details']) && $_REQUEST['details']) {
		$html_command .= " -d";
	}
	// echo "<p>$html_command</p>";
	shell_exec($html_command);

	$content = file_get_contents($html);
	$content = str_replace("http://www.newsreader-project.eu/data/cars", "...", $content);
	echo '<div id="treecontrol" style="display: block;">
		<a title="Collapse the entire tree below" href="#"><img src="js/treeview/images/minus.gif"> Collapse All</a>
		&middot;
		<a title="Expand the entire tree below" href="#"><img src="js/treeview/images/plus.gif"> Expand All</a>
	</div>';

	echo "<div class='well' id='navigation-container'>";
	echo $content;
	echo "</div>";
}
else {
	echo "<p>Select a file on the left and press the 'Send' button.</p>";
}

$Text = ob_get_clean();

include "template.inc.php";
