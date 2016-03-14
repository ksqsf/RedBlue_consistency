#used to create graphs for one or multiple experiments
import os, sys
import subprocess
import math
import re

lib_path = os.path.abspath('./')
sys.path.append(lib_path)
from util import *


def getmaxY(fileList):
    #get the x scale
    maxX = 0.0
    maxY = 0.0
    for y in fileList:
        print y
        f = open(y, 'r')
        lines = f.readlines()
        f.close()
        for line in lines:
            tmpList = line.split(" ")
	    tmpList = removeNoneFromList(tmpList)
            print  tmpList
            if maxX < float(tmpList[xCol-1]):
                maxX = float(tmpList[xCol-1])
            if maxY < float(tmpList[yCol-1]):
                maxY = float(tmpList[yCol-1])
        print maxX, maxY
    return maxX, maxY

def generate_gnu(fileList):
    #generate gnu file
    graphFolder = "./"
    graphFolderPath = graphFolder + graphFolderName
    if not os.path.exists(graphFolderPath):
        os.mkdir(graphFolderPath)
    gnuFile = "thla"+graphFileName+".gnu"
    os.system("touch "+graphFolderPath+"/"+gnuFile)
    tf = open(graphFolderPath+"/"+gnuFile, "w+")
    maxX, maxY = getmaxY(fileList)
    maxX = 10 - round(maxX)%10 + round(maxX)
    maxY = 10 - round(maxY)%10 + round(maxY)
    print maxX, maxY
    titleStr = "set title \"" +graphName+"\"\n"
    tf.write(titleStr)
    xLabelStr = "set xlabel \"X (XXX)\"\n"
    tf.write(xLabelStr)
    yLabelStr = "set ylabel \"Y (YYY)\"\n"
    tf.write(yLabelStr)
    xRangeStr = "set xrange[0:"+str(int(maxX)) +"]\n"
    tf.write(xRangeStr)
    yRangeStr = "set yrange[0:"+str(int(maxY))+"]\n"
    tf.write(yRangeStr)
    termStr = "set term postscript eps enhanced color\n"
    tf.write(termStr)
    outputStr = "set output \""+graphFolderPath+"/th_la"+graphFileName+".ps\"\n"
    tf.write(outputStr)
    plotStr = "plot "
    
    for x in fileList:
        plotStr += "'"+x+"' using "+str(xCol)+":"+str(yCol) +" title 'TBD' with linespoints lw 3,"
    tmpStr = plotStr[0:(len(plotStr)-1)]
    tf.write(tmpStr)

if __name__ == '__main__':
   if len(sys.argv) != 7:
      print "Usage: python thlaUserGraph.py fileList graphName graphFolderName graphFileName xCol yCol\n"
      sys.exit()
      
   print "That is a script to generate graphs of throughput and latency at user side\n"
   option = sys.argv[1:]
   fileStr = option[0]
   fileList = fileStr.split(" ")
   print fileList
   graphName = option[1]
   graphFolderName = option[2]
   graphFileName = option[3]
   xCol = int(option[4])
   yCol = int(option[5])
   
   generate_gnu(fileList)
   
