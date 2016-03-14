import os,sys
from time import time,sleep
from util import *

def compile_gnu(file):
    fileStr = getFileName(file)
    fileStr = fileStr[0:(len(fileStr)-4)]
    print fileStr
    
    dirStr = checkDir(getDirFromFile(file))
    
    
    command = "sed -i '/set output/ s/\".*\"/\""+ fileStr + ".ps"+"\"/g' " + file
    print command
    os.system(command)
    
    
    command = "gnuplot "+ file
    print command
    os.system(command)
    
    
    command = "ps2pdf " + fileStr + ".ps"
    print command
    os.system(command)
    
    command = "./pdfcrop.pl " + fileStr + ".pdf"
    print command
    os.system(command)
    
    command = "mv " + fileStr + "-crop.pdf " + fileStr + ".pdf"
    print command
    os.system(command)
    
    command = "mv " + fileStr + ".pdf "  + dirStr
    print command
    os.system(command)
    
    command = "rm " + fileStr + ".ps"
    print command
    os.system(command)
    

def get_all_gnu_file(dirList):
    gnuFileList = list()
    
    for x in dirList:
        command = "find "+ x +" | grep \"\.gnu\""
        f = os.popen(command)
        for i in f.readlines():
            print i
            i = removeNewLine(i)
            gnuFileList.append(i)
        f.close()
    print gnuFileList
    return gnuFileList

def compile_gnu_all_dir(dirList):
    fileList = get_all_gnu_file(dirList)
    
    for x in fileList:
        compile_gnu(x)
    
           
if __name__ == '__main__':
    
    if len(sys.argv) != 2:
      print "Usage: python compileGnu.py dirList\n"
      sys.exit()
    
    option = sys.argv[1:]
    print option[0]
    dirStr = option[0]
    dirList = get_dir_list(dirStr)
    compile_gnu_all_dir(dirList)
