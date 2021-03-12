import subprocess
import os
import time
import configparser
import shutil
import glob



config = configparser.ConfigParser()
storylist = []
_input = "placeholder"
bulk = ""

#-------------

if os.path.isfile('RoyalConfig.ini') != True:
    print ("No config found, creating one.")
    
    config['SETTINGS'] = {'Path To Jar Directory': "",
                          'Jar file name': "",
                          'Download destination': "" }

    
    with open('RoyalConfig.ini', 'w') as configfile:
        config.write(configfile)
    print("default settings loaded, please set config correctly, exiting in five seconds")
    time.sleep(5)
    exit()

else:
    #print ("Config found!")
    config.read("RoyalConfig.ini")

    settings = config['SETTINGS']
    _dir = settings['Path To Jar Directory']
    _jar = settings['Jar file name']
    _down = settings['Download destination']
    
    #sanity check
    if _dir == "":
        print('You need to set the .jar path! exiting in 5 seconds')
        time.sleep(5)
        exit()
    if _jar == "":
        print('You need to set the .jar name! exiting in 5 seconds')
        time.sleep(5)
        exit()
    if _down == "":
        print('Download destination not set. Defaulting to desktop folder')
        _downtemp = os.path.expanduser("~/Desktop")
        #print(_downtemp)
        
        if os.path.exists(os.path.join(_downtemp, "RoyalRoadDownloads")):
            _down = os.path.join(_downtemp, "RoyalRoadDownloads")
        else:
            os.mkdir(os.path.join(_downtemp, "RoyalRoadDownloads"))
            _down = os.path.join(_downtemp, "RoyalRoadDownloads")
            

            
    if os.path.exists(_down):
        print("Putting books at {}".format(_down))
    else:
        print("Unable to find the download location specified! exiting in 5 seconds")
        time.sleep(5)
        exit()
        
    if os.path.exists(_dir):
        print("Directory of .jar is ", _dir)
    else:
        print("Unable to fine {}! exiting in 5 seconds!".format(_dir))
        time.sleep(5)
        exit()

    if os.path.isfile(_jar):
        print(".jar is ", _jar)
    else:
        print("Unable to find {} at {}! exiting in 5 seconds".format(_jar, _dir))
        time.sleep(5)
        exit()

    #-------------

time.sleep(2)
#print(_down)
if os.path.exists(_dir):
    os.chdir(_dir)
else:
    print("There is no such director {}".format(_dir))
    time.sleep(5)
    exit()
print("Enter story URL, submit nothing to begin")

while _input != "":
    for _name in storylist:
        print(_name)
    _input = input()
    storylist.append(_input)

if '' in storylist:
    storylist.remove('')
    
print("Downloading {}".format(storylist))
print ("")


for _link in storylist:
    cmd = "java -jar {} {}".format(_jar, _link)
    

    print("Downloading {} this may take a while depending on the length of the book".format(_link))
    res = subprocess.call(cmd, shell = True)
    time.sleep(2)
    if res == 0:
        print("Book downloaded")
        print('')
    else:
        print("There has been an error in the .jar file or config! make sure it is set up correctly and uncorrupted!")

print("All Books downloaded!")


for file in glob.glob(_dir + r"/*html"):
    print(file)
    shutil.copy(file, _down)
    os.remove(file)

print("Done!")
time.sleep(5)
