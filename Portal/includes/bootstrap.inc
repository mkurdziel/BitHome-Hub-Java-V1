<?php

/**
 * @file
 * Functions that need to be loaded on every SyNet Node request.
 */



/**
 * Find the appropriate configuration directory.
 *
 * Try finding a matching configuration directory by stripping the website's
 * hostname from left to right and pathname from right to left. The first
 * configuration file found will be used; the remaining will ignored. If no
 * configuration file is found, return a default value '$confdir/default'.
 *
 * Example for a fictitious site installed at
 * http://www.drupal.org:8080/mysite/test/ the 'settings.php' is searched in
 * the following directories:
 *
 *  1. $confdir/8080.www.drupal.org.mysite.test
 *  2. $confdir/www.drupal.org.mysite.test
 *  3. $confdir/drupal.org.mysite.test
 *  4. $confdir/org.mysite.test
 *
 *  5. $confdir/8080.www.drupal.org.mysite
 *  6. $confdir/www.drupal.org.mysite
 *  7. $confdir/drupal.org.mysite
 *  8. $confdir/org.mysite
 *
 *  9. $confdir/8080.www.drupal.org
 * 10. $confdir/www.drupal.org
 * 11. $confdir/drupal.org
 * 12. $confdir/org
 *
 * 13. $confdir/default
 *
 * @param $require_settings
 *   Only configuration directories with an existing settings.php file
 *   will be recognized. Defaults to TRUE. During initial installation,
 *   this is set to FALSE so that Drupal can detect a matching directory,
 *   then create a new settings.php file in it.
 * @param reset
 *   Force a full search for matching directories even if one had been
 *   found previously.
 * @return
 *   The path of the matching directory.
 */
function conf_path($require_settings = TRUE, $reset = FALSE) {
  static $conf = '';

  if ($conf && !$reset) {
    return $conf;
  }

  $confdir = 'etc';
  $uri = explode('/', $_SERVER['SCRIPT_NAME'] ? $_SERVER['SCRIPT_NAME'] : $_SERVER['SCRIPT_FILENAME']);
  $server = explode('.', implode('.', array_reverse(explode(':', rtrim($_SERVER['HTTP_HOST'], '.')))));
  for ($i = count($uri) - 1; $i > 0; $i--) {
    for ($j = count($server); $j > 0; $j--) {
      $dir = implode('.', array_slice($server, -$j)) . implode('.', array_slice($uri, 0, $i));
      if (file_exists("$confdir/$dir/settings.php") || (!$require_settings && file_exists("$confdir/$dir"))) {
        $conf = "$confdir/$dir";
        return $conf;
      }
    }
  }
  $conf = "$confdir";
  return $conf;
}

/**
 * Unsets all disallowed global variables. See $allowed for what's allowed.
 */
function synet_unset_globals() {
  if (ini_get('register_globals')) {
    $allowed = array('_ENV' => 1, '_GET' => 1, '_POST' => 1, '_COOKIE' => 1, '_FILES' => 1, '_SERVER' => 1, '_REQUEST' => 1, 'GLOBALS' => 1);
    foreach ($GLOBALS as $key => $value) {
      if (!isset($allowed[$key])) {
        unset($GLOBALS[$key]);
      }
    }
  }
}

/**
 * Validate that a hostname (for example $_SERVER['HTTP_HOST']) is safe.
 *
 * As $_SERVER['HTTP_HOST'] is user input, ensure it only contains characters
 * allowed in hostnames.  See RFC 952 (and RFC 2181). $_SERVER['HTTP_HOST'] is
 * lowercased.
 *
 * @return
 *  TRUE if only containing valid characters, or FALSE otherwise.
 */
function synet_valid_http_host($host) {
  return preg_match('/^\[?(?:[a-z0-9-:\]_]+\.?)+$/', $host);
}

/**
 * Loads the configuration and sets the base URL, cookie domain, and
 * session name correctly.
 */
function conf_init() {
  global $base_url, $base_path, $base_root;

  // Export the following settings.php variables to the global namespace
  global $db_url, $db_prefix, $db_collation, $cookie_domain, $conf, $installed_profile, $update_free_access;
  $conf = array();

  if (!isset($_SERVER['SERVER_PROTOCOL']) || ($_SERVER['SERVER_PROTOCOL'] != 'HTTP/1.0' && $_SERVER['SERVER_PROTOCOL'] != 'HTTP/1.1')) {
    $_SERVER['SERVER_PROTOCOL'] = 'HTTP/1.0';
  }

  if (isset($_SERVER['HTTP_HOST'])) {
    // As HTTP_HOST is user input, ensure it only contains characters allowed
    // in hostnames. See RFC 952 (and RFC 2181).
    // $_SERVER['HTTP_HOST'] is lowercased here per specifications.
    $_SERVER['HTTP_HOST'] = strtolower($_SERVER['HTTP_HOST']);
    if (!synet_valid_http_host($_SERVER['HTTP_HOST'])) {
      // HTTP_HOST is invalid, e.g. if containing slashes it may be an attack.
      header($_SERVER['SERVER_PROTOCOL'] .' 400 Bad Request');
      exit;
    }
  }
  else {
    // Some pre-HTTP/1.1 clients will not send a Host header. Ensure the key is
    // defined for E_ALL compliance.
    $_SERVER['HTTP_HOST'] = '';
  }

  if (file_exists('./'. conf_path() .'/settings.php')) {
    include_once './'. conf_path() .'/settings.php';
  }

  if (isset($base_url)) {
    // Parse fixed base URL from settings.php.
    $parts = parse_url($base_url);
    if (!isset($parts['path'])) {
      $parts['path'] = '';
    }
    $base_path = $parts['path'] .'/';
    // Build $base_root (everything until first slash after "scheme://").
    $base_root = substr($base_url, 0, strlen($base_url) - strlen($parts['path']));
  }
  else {
    // Create base URL
    $base_root = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] == 'on') ? 'https' : 'http';

    $base_url = $base_root .= '://'. $_SERVER['HTTP_HOST'];

    // $_SERVER['SCRIPT_NAME'] can, in contrast to $_SERVER['PHP_SELF'], not
    // be modified by a visitor.
    if ($dir = trim(dirname($_SERVER['SCRIPT_NAME']), '\,/')) {
      $base_path = "/$dir";
      $base_url .= $base_path;
      $base_path .= '/';
    }
    else {
      $base_path = '/';
    }
  }

  if ($cookie_domain) {
    // If the user specifies the cookie domain, also use it for session name.
    $session_name = $cookie_domain;
  }
  else {
    // Otherwise use $base_url as session name, without the protocol
    // to use the same session identifiers across http and https.
    list( , $session_name) = explode('://', $base_url, 2);
    // We escape the hostname because it can be modified by a visitor.
    if (!empty($_SERVER['HTTP_HOST'])) {
      $cookie_domain = check_plain($_SERVER['HTTP_HOST']);
      // Strip leading periods, www., and port numbers from cookie domain.
      $cookie_domain = ltrim($cookie_domain, '.');
      if (strpos($cookie_domain, 'www.') === 0) {
        $cookie_domain = substr($cookie_domain, 4);
      }
      $cookie_domain = explode(':', $cookie_domain);
      $cookie_domain = '.'. $cookie_domain[0];
    }
  }
  // To prevent session cookies from being hijacked, a user can configure the
  // SSL version of their website to only transfer session cookies via SSL by
  // using PHP's session.cookie_secure setting. The browser will then use two
  // separate session cookies for the HTTPS and HTTP versions of the site. So we
  // must use different session identifiers for HTTPS and HTTP to prevent a
  // cookie collision.
  if (ini_get('session.cookie_secure')) {
    $session_name .= 'SSL';
  }
  // Per RFC 2109, cookie domains must contain at least one dot other than the
  // first. For hosts such as 'localhost' or IP Addresses we don't set a cookie domain.
  if (count(explode('.', $cookie_domain)) > 2 && !is_numeric(str_replace('.', '', $cookie_domain))) {
    ini_set('session.cookie_domain', $cookie_domain);
  }
  session_name('SESS'. md5($session_name));
}

/**
 * Load the persistent variable table.
 *
 * The variable table is composed of values that have been saved in the table
 * with variable_set() as well as those explicitly specified in the configuration
 * file.
 */
function requiredSettingDirFound($dirSpec = '') {
	$findStatus = FALSE;
	if(is_dir($dirSpec))
	{
		$findStatus = TRUE;
	}
	else
	{
		print("ERROR - dir [" . $dirSpec . "] not valid!\n");
	}
	return $findStatus;
}	

/**
 * Load the persistent variable table.
 *
 * The variable table is composed of values that have been saved in the table
 * with variable_set() as well as those explicitly specified in the configuration
 * file.
 */
function variable_init($conf = array()) {

  foreach ($conf as $name => $value) {
    $variables[$name] = $value;
  }

  return $variables;
}

/**
 * Returns a persistent variable.
 *
 * Case-sensitivity of the variable_* functions depends on the database
 * collation used. To avoid problems, always use lower case for persistent
 * variable names.
 *
 * @param $name
 *   The name of the variable to return.
 * @param $default
 *   The default value to use if this variable has never been set.
 * @return
 *   The value of the variable.
 *
 * @see variable_del(), variable_set()
 */
function variable_get($name, $default) {
  global $conf;

  return isset($conf[$name]) ? $conf[$name] : $default;
}

/**
 * Sets a persistent variable.
 *
 * Case-sensitivity of the variable_* functions depends on the database
 * collation used. To avoid problems, always use lower case for persistent
 * variable names.
 *
 * @param $name
 *   The name of the variable to set.
 * @param $value
 *   The value to set. This can be any PHP data type; these functions take care
 *   of serialization as necessary.
 *
 * @see variable_del(), variable_get()
 */
function variable_set($name, $value) {
  global $conf;

  $conf[$name] = $value;
}

/**
 * Unsets a persistent variable.
 *
 * Case-sensitivity of the variable_* functions depends on the database
 * collation used. To avoid problems, always use lower case for persistent
 * variable names.
 *
 * @param $name
 *   The name of the variable to undefine.
 *
 * @see variable_get(), variable_set()
 */
function variable_del($name) {
  global $conf;

  unset($conf[$name]);
}


/**
 * Includes a file with the provided type and name. This prevents
 * including a theme, engine, module, etc., more than once.
 *
 * @param $type
 *   The type of item to load (i.e. theme, theme_engine, module, profile).
 * @param $name
 *   The name of the item to load.
 *
 * @return
 *   TRUE if the item is loaded or has already been loaded.
 */
function synet_load($type, $name) {
  static $files = array();

  if (isset($files[$type][$name])) {
    return TRUE;
  }

  $filename = synet_get_filename($type, $name);

  if ($filename) {
    include_once "./$filename";
    $files[$type][$name] = TRUE;

    return TRUE;
  }

  return FALSE;
}

/**
 * Set HTTP headers in preparation for a page response.
 *
 * Authenticated users are always given a 'no-cache' header, and will
 * fetch a fresh page on every request.  This prevents authenticated
 * users seeing locally cached pages that show them as logged out.
 *
 * @see page_set_cache()
 */
function synet_page_header() {
  header("Expires: Sun, 19 Nov 1978 05:00:00 GMT");
  header("Last-Modified: ". gmdate("D, d M Y H:i:s") ." GMT");
  header("Cache-Control: store, no-cache, must-revalidate");
  header("Cache-Control: post-check=0, pre-check=0", FALSE);
}


/**
 * Unserializes and appends elements from a serialized string.
 *
 * @param $obj
 *   The object to which the elements are appended.
 * @param $field
 *   The attribute of $obj whose value should be unserialized.
 */
function synet_unpack($obj, $field = 'data') {
  if ($obj->$field && $data = unserialize($obj->$field)) {
    foreach ($data as $key => $value) {
      if (!empty($key) && !isset($obj->$key)) {
        $obj->$key = $value;
      }
    }
  }
  return $obj;
}

/**
 * Return the URI of the referring page.
 */
function referer_uri() {
  if (isset($_SERVER['HTTP_REFERER'])) {
    return $_SERVER['HTTP_REFERER'];
  }
}

/**
 * Encode special characters in a plain-text string for display as HTML.
 *
 * Also validates strings as UTF-8 to prevent cross site scripting attacks on
 * Internet Explorer 6.
 *
 * @param $text
 *   The text to be checked or processed.
 * @return
 *   An HTML safe version of $text, or an empty string if $text is not
 *   valid UTF-8.
 *
 * @see synet_validate_utf8().
 */
function check_plain($text) {
  static $php525;

  if (!isset($php525)) {
    $php525 = version_compare(PHP_VERSION, '5.2.5', '>=');
  }
  // We duplicate the preg_match() to validate strings as UTF-8 from
  // synet_validate_utf8() here. This avoids the overhead of an additional
  // function call, since check_plain() may be called hundreds of times during
  // a request. For PHP 5.2.5+, this check for valid UTF-8 should be handled
  // internally by PHP in htmlspecialchars().
  // @see http://www.php.net/releases/5_2_5.php
  // @todo remove this when support for either IE6 or PHP < 5.2.5 is dropped.

  if ($php525) {
    return htmlspecialchars($text, ENT_QUOTES, 'UTF-8');
  }
  return (preg_match('/^./us', $text) == 1) ? htmlspecialchars($text, ENT_QUOTES, 'UTF-8') : '';
}

/**
 * Checks whether a string is valid UTF-8.
 *
 * All functions designed to filter input should use synet_validate_utf8
 * to ensure they operate on valid UTF-8 strings to prevent bypass of the
 * filter.
 *
 * When text containing an invalid UTF-8 lead byte (0xC0 - 0xFF) is presented
 * as UTF-8 to Internet Explorer 6, the program may misinterpret subsequent
 * bytes. When these subsequent bytes are HTML control characters such as
 * quotes or angle brackets, parts of the text that were deemed safe by filters
 * end up in locations that are potentially unsafe; An onerror attribute that
 * is outside of a tag, and thus deemed safe by a filter, can be interpreted
 * by the browser as if it were inside the tag.
 *
 * This function exploits preg_match behaviour (since PHP 4.3.5) when used
 * with the u modifier, as a fast way to find invalid UTF-8. When the matched
 * string contains an invalid byte sequence, it will fail silently.
 *
 * preg_match may not fail on 4 and 5 octet sequences, even though they
 * are not supported by the specification.
 *
 * The specific preg_match behaviour is present since PHP 4.3.5.
 *
 * @param $text
 *   The text to check.
 * @return
 *   TRUE if the text is valid UTF-8, FALSE if not.
 */
function synet_validate_utf8($text) {
  if (strlen($text) == 0) {
    return TRUE;
  }
  // For performance reasons this logic is duplicated in check_plain().
  return (preg_match('/^./us', $text) == 1);
}

/**
 * Since $_SERVER['REQUEST_URI'] is only available on Apache, we
 * generate an equivalent using other environment variables.
 */
function request_uri() {

  if (isset($_SERVER['REQUEST_URI'])) {
    $uri = $_SERVER['REQUEST_URI'];
  }
  else {
    if (isset($_SERVER['argv'])) {
      $uri = $_SERVER['SCRIPT_NAME'] .'?'. $_SERVER['argv'][0];
    }
    elseif (isset($_SERVER['QUERY_STRING'])) {
      $uri = $_SERVER['SCRIPT_NAME'] .'?'. $_SERVER['QUERY_STRING'];
    }
    else {
      $uri = $_SERVER['SCRIPT_NAME'];
    }
  }
  // Prevent multiple slashes to avoid cross site requests via the FAPI.
  $uri = '/'. ltrim($uri, '/');

  return $uri;
}

/**
 * If Drupal is behind a reverse proxy, we use the X-Forwarded-For header
 * instead of $_SERVER['REMOTE_ADDR'], which would be the IP address
 * of the proxy server, and not the client's.
 *
 * @return
 *   IP address of client machine, adjusted for reverse proxy.
 */
function ip_address() {
  static $ip_address = NULL;

  if (!isset($ip_address)) {
    $ip_address = $_SERVER['REMOTE_ADDR'];
    if (variable_get('reverse_proxy', 0) && array_key_exists('HTTP_X_FORWARDED_FOR', $_SERVER)) {
      // If an array of known reverse proxy IPs is provided, then trust
      // the XFF header if request really comes from one of them.
      $reverse_proxy_addresses = variable_get('reverse_proxy_addresses', array());
      if (!empty($reverse_proxy_addresses) && in_array($ip_address, $reverse_proxy_addresses, TRUE)) {
        // If there are several arguments, we need to check the most
        // recently added one, i.e. the last one.
        $ip_address_parts = explode(',', $_SERVER['HTTP_X_FORWARDED_FOR']);
        $ip_address = array_pop($ip_address_parts);
      }
    }
  }

  return $ip_address;
}


/**
 * requests to this script are sometimes authorization-protected by
 *  requiring that a 'key' be passed.  This key must match that 
 *  generated by this script or the request will not be honored
 *
 * @return
 *   T/F where T means the request authorization key is valid!
 */
function validAuthKey($authKey) {
	$validStatus = FALSE;
	if($authKey == '4527' || $authKey == '') // this will go live later!
	{
		$validStatus = TRUE;
	}
	return $validStatus;	
}

/**
 * our SyNet daemon generates timestamped filenames which can
 *  be sorted during our list of files in directory to show
 *  which is the latest of a given fileset.
 *
 *  This function returns the latest of the files in the set
 *
 * @return
 *   filename of most recent file in set (or '' if no files in set)
 */
function firstMatchingFile($fileMatchPattern) {
	$desiredFile = '';
	
	// break match pattern into parts
	$pathParts = pathinfo($fileMatchPattern);
	
	// get listing of files from directory part
	$dirSpec = $pathParts['dirname'];
	$fileNames = scandir($dirSpec, SCANDIR_PARM_REVERSE_RESULTS);
	
	// break filename part into match prefix and suffix
	$matchSpec = $pathParts['basename'];
	$matchParts = explode('*', $matchSpec);
	// we want precisely 2 parts, not 3 or more...
	if(!isset($matchParts[0]) || !isset($matchParts[1]) || isset($matchParts[2]))
	{
		print("ERROR- too many *'s in filename match spec!<br>\n");
		return $desiredFile;
	}
	
	$namePrefix = $matchParts[0];
	$nameSuffix = $matchParts[1];
	
	foreach($fileNames as $fileName) 
	{
		//print ("- checking fileName=[" . $fileName . "]<br>\n");
		
		$filePrefix = substr($fileName, 0, strlen($namePrefix));
		//print ("&nbsp;-- checking filePrefix=[" . $filePrefix . "]<br>\n");
		
		$suffixLen = strlen($nameSuffix);
		$fileSuffix = substr($fileName, 0-$suffixLen, $suffixLen);
		//print ("&nbsp;-- checking fileSuffix=[" . $fileSuffix . "]<br>\n");
		
		if(($filePrefix == $namePrefix) && ($fileSuffix == $nameSuffix))
		{
			$desiredFile = $fileName;
			break;
		}
	}
	
  if($desiredFile == '')
  {
		print ("ERROR- no files found for request! fileMatchPattern=[" . $fileMatchPattern . "]\n");
  }
  return $desiredFile;
}

/**
 * our SyNet daemon generates timestamped filenames which can
 *  be sorted during our list of files in directory to show
 *  which is the latest of a given fileset.
 *
 *  This function returns the latest of the files in the set
 *
 * @return
 *   filename of most recent file in set (or '' if no files in set)
 */
function fileSpecOfDynamicFile($filename) {
	return INTERACTIVE_DIRECTORY . "/" . $filename;
}


/**
 * our nodes.php script receives multipart field values 
 *  in URLs
 *
 *  This function returns the left of a two-valued field
 *
 * @return
 *   the left part (value1) of a "value1,value2" fieldValue
 */
function leftValueOfCommaSplitField($fieldValue) {
	$valueParts = explode(',', $fieldValue);
	if(!isset($valueParts[0]) || !isset($valueParts[1]))
	{
		print("ERROR- too few comma-sep values in field-value [" . $fieldValue . "]!<br>\n");
		return '';
	}
	return $valueParts[0];
}

/**
 * our nodes.php script receives multipart field values 
 *  in URLs
 *
 *  This function returns the right of a two-valued field
 *
 * @return
 *   the right part (value2) of a "value1,value2" fieldValue
 */
function rightValueOfCommaSplitField($fieldValue) {
	$valueParts = explode(',', $fieldValue);
	if(!isset($valueParts[0]) || !isset($valueParts[1]))
	{
		print("ERROR- too few comma-sep values in field-value [" . $fieldValue . "]!<br>\n");
		return '';
	}
	return $valueParts[1];
}

/**
 * our SyNet DateTime: 
 *   Date/time formats are all in the format of "yyMMddHHmmssS"
 *
 *  where:  yy  = last two digits of the year (ex: 12 for 2012)
 *          MM = two digit month (ex: 01, 02, 03,  12)
 *          dd   = two digit day (ex: 01 - 31)
 *          HH  = two digit hour in 24-hour format (ex: 01 - 24)
 *          mm = two digit minute (ex: 01 - 60)
 *          ss   = two digit second (ex: 01 - 60)
 *          S    = one digit fraction of second (ex: 0-9)
 *
 * @return
 *   the right part (value2) of a "value1,value2" fieldValue
 */
function syNetTimeString() {
	$timeNow = getdate();
	
	$year = substr($timeNow['year'], 2, 2);	// last two digits
	$month = $timeNow['mon'];
	if(strlen($month) < 2) $month = "0" . $month;
	$day = $timeNow['mday'];
	if(strlen($day) < 2) $day = "0" . $day;
	$hour = $timeNow['hours'];
	if(strlen($hours) < 2) $hours = "0" . $hours;
	$minutes = $timeNow['minutes'];
	if(strlen($minutes) < 2) $minutes = "0" . $minutes;
	$seconds = $timeNow['seconds'];
	if(strlen($seconds) < 2) $seconds = "0" . $seconds;
	
	return $year . $month . $day . $hour . $minutes . $seconds . "0"; // no tenths so zero for last char
}

function unquotedValue($value) {
	$len = strlen($value);
	//print "<br>DBG value=[" . $value . "], len=[" . $len . "]<br>\n";
	
	$unquotedValue = $value;
		
	if($len >= 4)
	{
		$ltQuote = substr($value, 0, 2);
		$rtQuote = substr($value, $len-2, 2);
	  //print "<br>DBG 4l lt=[" . $ltQuote . "], rt=[" . $rtQuote . "]<br>\n";
		
		if(($ltQuote == "\\\"" || $ltQuote == "\\'") && $ltQuote == $rtQuote)
		{
			//print "<br>DBG unQuoting!<br>\n";
			$unquotedValue = substr($value, 2, $len-4);
		}
	}
	else if($len >= 2)
	{
		$ltQuote = substr($value, 0, 1);
		$rtQuote = substr($value, $len-1, 1);
	  //print "<br>DBG 2l lt=[" . $ltQuote . "], rt=[" . $rtQuote . "]<br>\n";
		
		if(($ltQuote == "\"" || $ltQuote == "'") && $ltQuote == $rtQuote)
		{
			//print "<br>DBG unQuoting!<br>\n";
			$unquotedValue = substr($value, 1, $len-2);
		}
	}
	
	
	//print "<br>DBG value=[" . $value . "], unquote=[" . $unquotedValue . "]<br>\n";
	return $unquotedValue;
}

