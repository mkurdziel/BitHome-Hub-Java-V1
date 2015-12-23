<?php

require_once './includes/bootstrap.inc';

date_default_timezone_set('America/Denver');

$confDir = conf_path(TRUE, TRUE);	// TRUE, TRUE = determine path anew
$settingsFSpec = "./". $confDir . "/settings.php";
//print ("settingFSpec=[" . $settingsFSpec . "]\n");
require_once $settingsFSpec;

if(!requiredSettingDirFound(INTERACTIVE_DIRECTORY))
{
	print ("ERROR- required directory NOT found [INTERACTIVE_DIRECTORY]<br>\n");
	exit();
}

// Add more action specific logic inside switch()
// Convert to UPPER CASE
$request_method = strtoupper($_SERVER['REQUEST_METHOD']);
switch ($request_method) {
	case 'GET':
		$action = 'lookup';	// search
		break;
	case 'POST':
		$action = 'add';	// add  -not used?-
		break;
	case 'PUT':
		$action = 'command';	// update
		break;
	case 'DELETE':
		$action = 'delete';	// delete  -not used?-
		break;
	default:
		// invalid action
		exit();
}

// ... handle actions here
if($action == 'lookup')
{
	$filePrefix = '';
	$authKey = '';
	$deviceID = '';
	$actionID = '';
	$uniqRequestID = 0;
	$subAction= "list";
	
	if(isset($_REQUEST['list']))
	{
		$authKey = $_REQUEST['list'];
		$filePrefix = "nodelist_";
	}
	else if(isset($_REQUEST['info']))
	{
		$authKey = $_REQUEST['info'];
		$filePrefix = "info_";
	}
	else if(isset($_REQUEST['catalog']))
	{
		$deviceID = $_REQUEST['catalog'];
		$filePrefix = "catalog_" . $deviceID . "_";
	}
	else if(isset($_REQUEST['set']))
	{
		$subAction= "set";
		$authKey = $_REQUEST['set'];
		if(isset($_REQUEST['act']))
		{
			$actionID = leftValueOfCommaSplitField($_REQUEST['act']);
			$uniqRequestID = rightValueOfCommaSplitField($_REQUEST['act']);
			$filePrefix = "actionRequest_" . sprintf("%05d",$actionID) . "_" . sprintf("%08d",$uniqRequestID) . ".xml";
		}
	}
	else if(isset($_REQUEST['resp']))
	{
		$subAction= "resp";
		$authKey = $_REQUEST['resp'];
		if(isset($_REQUEST['act']))
		{
			$actionID = leftValueOfCommaSplitField($_REQUEST['act']);
			$uniqRequestID = rightValueOfCommaSplitField($_REQUEST['act']);
			$filePrefix = "actionResponse_" . sprintf("%05d",$actionID) . "_" . sprintf("%08d",$uniqRequestID) . ".xml";
		}
	}

	if(!validAuthKey($authKey))
	{
		header('Content-Type: text/plain');
		print ("ERROR- request NOT authorized\n");
		exit();
	}
	
	if($subAction == 'list')
	{
		
		$fileMatchPattern = fileSpecOfDynamicFile($filePrefix . "*.xml");
		$desiredFile = firstMatchingFile($fileMatchPattern);
	  if($desiredFile == '')
	  {
  		header('Content-Type: text/plain');
			print ("ERROR- No files in set! fileMatchPattern=[" . $fileMatchPattern .  "]<br>\n");
			exit();
	  }
	  
		//print ("FOUND! desiredFile=[" . $desiredFile . "]\n");
		$desiredFileSpec = fileSpecOfDynamicFile($desiredFile);
		if(!is_file($desiredFileSpec))
		{
  		header('Content-Type: text/plain');
			print ("ERROR- File NOT found! desiredFileSpec=[" . $desiredFileSpec .  "]<br>\n");
			exit();
		}
	  
		header('Content-Type: text/xml');
		readfile($desiredFileSpec);
	}
	else if($subAction == 'set')
	{
		// setup our request attributes
		$actionAr = array('actionId' => "$actionID",
		                  'timeRequested' => syNetTimeString() );
		                 
		// here to issue command to device
		
		// add path to file basename
		$rqstFileName = $filePrefix;
		$rqstFileSpec = fileSpecOfDynamicFile("queries/" . $rqstFileName);
		// open file for writing
		$fh = fopen($rqstFileSpec,'w') or die("FILE-ERROR can't open [" . $rqstFileName . "]: $php_errormsg<br>\n");
		
		$nextLine = '<?xml version="1.0" encoding="ISO-8859-1"?>' . "\n";
		// emit the opening line to file
		fwrite($fh, $nextLine);
		
		// write first line of file
		$nextLine = '<actionRequest';
		// for each attribute
		foreach($actionAr as $tag => $data) {
			// append it and its value to the line
			$nextLine = $nextLine . " " . $tag . "=\"" . htmlspecialchars($data) . "\"";
		}
		// terminate the line
		$nextLine = $nextLine . ">\n";
		// emit the next data line to file
		fwrite($fh, $nextLine);
		
		// write arbitrary number of parameter lines to file
		foreach($_REQUEST as $tag => $data) {
			// if is parameter value...
			if($tag != 'set' && $tag != 'act') {
				// separate ID from value (comma delimited)
				$paramID = leftValueOfCommaSplitField($data);
				$paramValue = rightValueOfCommaSplitField($data);
				// remove any enclosing quotes...
				$paramValue = unquotedValue($paramValue);
				// emit parameter line to file
				$nextLine = "\t<parameter id=\"" . $paramID . "\" value=\"" . htmlspecialchars($paramValue). "\"/>\n";
				fwrite($fh, $nextLine);
			}
		}
		
		// write last line of file...
		$nextLine = "</actionRequest>\n";
		fwrite($fh, $nextLine);
		
		// force all content out to file
		fflush($fh);
		
		// close the file...
		fclose($fh) or die("FILE-ERROR can't close [" . $rqstFileName . "]: $php_errormsg\n");
		
		header('Content-Type: text/plain');
		print ("OK\n");
	}
	else if($subAction == 'resp')
	{
		// determine if a reponse for this action is yet present
		$desiredFile = $filePrefix;
		//print ("FOUND! desiredFile=[" . $desiredFile . "]\n");
		$desiredFileSpec = fileSpecOfDynamicFile("queries/" . $desiredFile);
		if(!is_file($desiredFileSpec))
		{
			print ("ERROR- File NOT found! desiredFileSpec=[" . $desiredFileSpec .  "]\n");
			exit();
		}
	  
		header('Content-Type: text/xml');
		readfile($desiredFileSpec);
		
		$DEBUGfSpec = fileSpecOfDynamicFile("queries/DEBUG.flg");
		
		if(!is_file($DEBUGfSpec))
		{
			unlink($desiredFileSpec);
			if(is_file($desiredFileSpec))
			{
				print ("ERROR- Failed to remove file! desiredFileSpec=[" . $desiredFileSpec .  "]\n");
				exit();
			}
		}
	}
	else
	{
		header('Content-Type: text/plain');
		print ("SUB-ACTION != [list|set]\n");
	}
}
else
{
	header('Content-Type: text/plain');
	print ("ACTION != lookup\n");
}

?>

