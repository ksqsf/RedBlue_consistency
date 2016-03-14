import re
import os,sys

def isFloat(str):
    try:
        float(str)
        return True
    except ValueError:
        return False
    
def isInt(str):
    try:
        int(str)
        return True
    except ValueError:
        return False

def getUserNum(file):
    tmpList = re.findall(r'\d+', file)
    #print tmpList
    return int(tmpList[0])

def getInt(file):
    tmpList = re.findall(r'\d+', file)
    #print tmpList
    return int(tmpList[0])
    
def getRatio(file):
    tmpList = re.findall(r"([0-9]\.\d+)", file)
    #print tmpList
    return float(tmpList[0])

def getRatioStr(ratio):
    intRatio = int(ratio * 10)
    return str(intRatio)

def checkInList(elem, tmpList):
    for x in tmpList:
        if float(x) == elem:
            return True
    return False

def checkDir(path):
    if path[len(path) - 1] <> '/':
        path = path + "/"
    return path

def get_dir_list(dir_str):
    tmp_list = dir_str.split(' ')
    count = 0;
    while count < len(tmp_list):
        if tmp_list[count][len(tmp_list[count]) - 1] <> '/':
            tmp_list[count] = tmp_list[count] + "/"
        count = count + 1
    #print tmp_list
    return tmp_list 

def get_child_dir_list(dir):
    dir = checkDir(dir)
    dir_list = list()
    if os.path.isdir(dir) == True:
        tmpList = os.listdir(dir)
        #print tmpList
        for x in tmpList:
            #print x
            if os.path.isdir(dir+x) == True:
                dir_list.append(dir+x)
    return dir_list

def getDirFromFile(file):
    if file[len(file)-1] == '/':
        file = file[0:(len(file)-1)]
    #print file
    tmp_list = file.split('/')
    if len(tmp_list) == 0:
        return "."
    else:
        dir = ""
        del tmp_list[-1]
        for x in tmp_list:
            dir += x + '/'
        return dir

def getFileName(file):
    if file[len(file)-1] == '/':
        file = file[0:(len(file)-1)]
    #print file
    tmp_list = file.split('/')
    if len(tmp_list) == 0:
        return file
    else:
        return tmp_list[-1]
    
def removeNewLine(str):
    if str[len(str) - 1] == '\n':
        str = str[0:(len(str)-1)]
    return str

def getRatioString(num):
    ratio = str(num)
    if ratio == "0.0":
        return "AllRed"
    elif ratio == "1.0":
        return "AllBlue"
    elif ratio == "0.5":
        return "50R/50B"
    
def uniqueInsert(tmpList, x):
    for y in tmpList:
        if y == x:
            return tmpList
    tmpList.append(x)
    return tmpList

def removeNoneFromList(tmpList):
    listCopy = list()
    for x in tmpList:
        if x <> '':
            listCopy.append(x)
    return listCopy
            
    