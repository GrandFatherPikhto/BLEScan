#!/usr/bin/python
#

from operator import mod
import re
import os
import datetime
import hashlib
from pathlib import Path
from sys import stdout

input_name     = "README.md"

class parse_links_md:
    def __init__(self, input, 
        links = None, 
        output = None, 
        handled = None, 
        unhandled = None, 
        empty = None,
        temp_dir = None,
        backup_dir = None) -> None:
        self.pattern = re.compile(r"(?P<name>\[.*?\])\s*(?P<link>\(.*?\)+)")
        self.empt_pattern = re.compile(r"^\s*\(\s*\)\s*$")

        self.create_dirs(temp_dir, backup_dir)

        self.input_md = input

        if links:
            self.links_md = links
        else:
            self.links_md = self.temp_dir + "/LINKS.md"

        if output:
            self.output_md    = self.temp_dir + "/" + output
        else:
            self.output_md = self.temp_dir + "/PROCEDED.md"

        if handled:
            self.handled_md   = self.temp_dir + "/" + handled
        else:
            self.handled_md = self.temp_dir + "/HANDLED.md"
        if unhandled:
            self.unhandled_md = self.temp_dir + "/" + unhandled
        else:
            self.unhandled_md = self.temp_dir + "/UNHANDLED.md"

        if empty:
            self.empty_md = self.temp_dir + "/" + empty
        else:
            self.empty_md = self.temp_dir + "/EMPTY.md"

        self.links        = {}
        self.proceded     = {}

        self.open_handles()
        self.parse_links()
        self.parse_input()

        self.save_links()

        self.end_rename()

        pass

    def __del__(self):
        self.input_handle.close()
        self.output_handle.close()
        self.links_handle.close()
        if self.handled_handle:
            self.handled_handle.close()
        if self.unhandled_handle:
            self.unhandled_handle.close()
        if self.empty_md:
            self.empty_handle.close()
        pass

    def mkdir(self, path):
        os_path = Path(path)
        os_path.mkdir(parents=True, exist_ok=True)
        pass

    def create_dirs(self, temp_dir, backup_dir):
        if temp_dir:
            self.temp_dir = temp_dir
        else:
            self.temp_dir = "./.linktemp"

        if backup_dir:
            self.backup_dir = backup_dir
        else:
            self.backup_dir = self.temp_dir + "/.backup"

        self.mkdir(self.temp_dir)
        self.mkdir(self.backup_dir)

        pass

    def open_handles(self):
        self.input_handle  = open(self.input_md,  "r")
        self.links_handle  = open(self.links_md, "r")
        self.output_handle = open(self.output_md, "w")

        if self.handled_md:
            self.handled_handle = open(self.handled_md, "w")
        else:
            self.handled_handle = None

        if self.unhandled_md:
            self.unhandled_handle = open(self.unhandled_md, "w")
        else:
            self.unhandled_handle = None
        
        if self.empty_md:
            self.empty_handle = open(self.empty_md, "w")
        else:
            self.empty_handle = None

        pass

    def parse_links(self):
        for link in self.links_handle:
            match = self.pattern.match(link)
            if match:
                self.links[match.group('name')] = match.group('link')
        pass
    
    def link_replace(self, match):
        name = match.group('name')
        link = match.group('link')

        if name not in link:
            self.proceded[name] = link
        elif not self.empt_pattern.match(link):
            self.proceded[name] = link

        if match.group('name') in self.links.keys():
            llink = self.links[name]
            return name + llink

        return match.group('name') + match.group('link')

    def print_link(self, name, link, output):
        fout = stdout
        if output:
            fout = output

        print(name + link, file=fout)
        pass

    def save_links(self):
        for name in self.proceded.keys():
            link = self.proceded[name]
            if self.empt_pattern.match(link):
                self.print_link(name, link, self.empty_handle)
            else:
                self.print_link(name, link, self.handled_handle)

            if name not in self.links.keys() and not self.empt_pattern.match(link):
                if self.unhandled_handle:
                    self.print_link(name, link, self.unhandled_handle)
        pass

    def parse_input(self):
        for line in self.input_handle:
            str = self.pattern.sub(self.link_replace, line)
            print(str, file = self.output_handle, end="")
        pass

    def hash_name(self, name):
        curtime = "{name}{datetime}".format(name = name, 
            datetime = datetime.datetime.now())
        md5name = hashlib.md5(curtime.encode()) 
        return self.backup_dir + "/." + md5name.hexdigest() + ".md"


    def end_rename(self):
        os.rename(self.input_md, self.hash_name(self.input_md))
        os.rename(self.output_md, self.input_md)
        pass

parse_links_md(input = input_name)

