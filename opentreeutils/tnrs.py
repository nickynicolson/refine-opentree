import urllib
import urllib2

def matchnames(name):
  res=''
  try:
    # Base url for the TNRS match_names API
    url = 'http://api.opentreeoflife.org/v2/tnrs/match_names'
    # Encode data value to be looked up as an array of names:
    data = '{"names": ["'+name+'"]}'
    print "Looking up: " + data
    # Set HTTP headers:
    headers = { 'Content-Type' : 'application/json' }
    # Make the call using HTTP POST
    req = urllib2.Request(url, data, headers)
    response = urllib2.urlopen(req)
    # Return the JSON value
    res = response.read()
  except (Error):
    print 'Exception occurred'
  return res

def echo(name):
  return name
