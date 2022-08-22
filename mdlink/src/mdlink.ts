import { existsSync, linkSync, mkdir, PathLike, readFile, writeFile } from 'node:fs';
import { ReadableStreamDefaultController } from 'stream/web';
import { ParsedPath } from 'node:path';
import path = require('node:path');
import { Url } from 'node:url';
import { Md5 } from 'md5-typescript'

interface MdLink {
	name: String;
	url: string | null;
};

interface MdNumLink {
	number: Number;
	name: String;
	url: string | null;
};

interface InitParams {
    mdFileName: PathLike,
	mdLinkDirectory: PathLike,
	mdTempDirectory: PathLike,
	mdHandledFile: PathLike,
	mdUnhandledFile: PathLike,
	mdLinksFile: PathLike,
	mdCurrentFile: PathLike,
};

interface MdVariables {
	links: MdNumLink[];
	handled: MdNumLink[];
	unhandled: MdNumLink[];
	current: MdNumLink[];
};

const initParams: InitParams = {
    mdFileName: '../README.md',
	mdLinkDirectory: './.mdlink',
	mdTempDirectory: './.mdlink/.temp',
	mdHandledFile: './.mdlink/.handled.json',
	mdUnhandledFile: './.mdlink/.unhandled.json',
	mdLinksFile: './.mdlink/.links.json',
	mdCurrentFile: './.mdlink/.current.json',
};

const mdVariables: MdVariables = {
	links: [],
	handled: [],
	unhandled: [],
	current: [],
};

function pushUnique(array:MdLink[], element: MdLink) : boolean {
    if(!array.some(item => item.name == element.name)) {
        array.push(element);
        return true;
    }
    return false;
}

async function parseMdLinks(document:string | null, result: 
    ((error: Error | null, links:MdLink[]) => void) | null = null)  {
    console.log('parseMdLinks()')
    if (document) {
        const matches:IterableIterator<RegExpMatchArray> 
            = document.matchAll(/(?<name>\[.*?\])\s*(?<link>\(.*?\))/g)
        const links:MdLink[] = [];

        Array.from(matches).map((match:RegExpMatchArray, index:Number, array) => {
            if (match.groups) {
                const name:string = match.groups['name'].replace(/^\[\s*|\]\s*$/g, '')
                let   link:string | null = match.groups['link'].replace(/^\(\s*|\)\s*$/g, '')
                const mdLink:MdLink = {name: name, url: link || null};
                pushUnique(links, mdLink);
            }
        })
        if (result != null) {
            result(null, links);
        }
    } else {
        if (result != null) {
            result(null, []);
        }
    }
}

async function parseLinks(handled: MdLink[], result: (error:Error | null, links:MdLink[] | null) => void) {
    if (existsSync(initParams.mdLinksFile)) {
        readFile(initParams.mdLinksFile, { encoding: 'utf-8'}, (errorReadLinks, documentLinks) => {
            // if (errorLinks) throw new Error('Ошибка открытия файла ' + initParams.mdLinksFile + ' [' + errorLinks + ']');
            if (!errorReadLinks) {
                try {
                    const links:MdLink[] = JSON.parse(documentLinks);
                    const concatLinks = links.concat(handled.filter( hand => !links.some(link => link.name === hand.name)));
                    
                    writeFile(initParams.mdLinksFile, JSON.stringify(concatLinks, null, 3), { encoding: 'utf-8'}, (errWriteLinks) => {
                        if (errWriteLinks)
                            throw new Error('Ошибка записи файла ' + initParams.mdLinksFile + ' [' + errWriteLinks + ']');
                    });

                    result(null, concatLinks);

                } catch (e) {
                    let message:String = "";
                    if (typeof e === 'string') {
                        message = e;
                    } else if (e instanceof Error) {
                        message = e.message;
                    }

                    result(Error('Ошибка разбора файла ' + initParams.mdLinksFile + ' [' + message + ']'), null);
                }
            }
        })

    } else {
        result(null, handled);
    }
}

async function writeLinks(fileName: PathLike, links:MdLink[], result: (error: Error | null) => void) {
    writeFile(fileName, JSON.stringify(links, null, 3), { encoding: 'utf-8'}, (err) => {
        if (err) {
            result(new Error('Ошибка записи файла ' + initParams.mdHandledFile + ' [' + err + ']'))
        } else
            result(null)
    })
}

async function prepareLinks(links:MdLink[], result: (error: Error | null, links: MdLink[]) => void) {
    console.log('prepareLinks()')
    const handled = links.filter(it => it.url)
    const unhandled = links.filter(it => !it.url)
    writeLinks(initParams.mdHandledFile, handled, (err) => {
        if(err) throw err
    })
    writeLinks(initParams.mdUnhandledFile, unhandled, (err) => {
        if(err) throw err
    })

    parseLinks(handled, (errorReadLinks, links) => {
        if (!errorReadLinks && links) {
            writeLinks(initParams.mdLinksFile, links, (error) => {
                if(error) {
                    result(error, [])
                    throw Error('Ошибка записи файла ' + initParams.mdLinksFile + ' [' + error?.message + ']')
                }
            })
            result(null, links)
        } else {
            
        }
    })
}

function escapeRegExp(string:String) {
    return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'); // $& means the whole matched string
}

async function fillMdLinks(links:MdLink[]) {
    readFile(initParams.mdFileName, { encoding: 'utf-8'}, (error, document) => {
        if (error) throw Error('Ошибка чтения файла ' + initParams.mdFileName + ' [' + error?.message + ']')
        let newDoc = document
        links.forEach(element => {
            const rgx  = '\\[\\s*' + escapeRegExp(element.name) + '\\s*\\]\\s*\\(\\s*\\)'
            const link = '[' + element.name + '](' + element.url + ')'        
            newDoc = newDoc.replace(new RegExp(rgx, 'gm'), link)
        })

        const backFileName = initParams.mdTempDirectory + '/.' + Md5.init(Date.now())
        writeFile(backFileName, document, {encoding: 'utf-8'}, (error) => {
            if (error)
                throw Error('Ошибка записи файла ' + backFileName + ' [' + error?.message + ']')
        })
        writeFile(initParams.mdFileName, newDoc, {encoding: 'utf-8'}, (error) => {
            if (error)
                throw Error('Ошибка записи файла ' + initParams.mdFileName + ' [' + error?.message + ']')
        })
    })
}

async function readMd() {
    const fileName = process.argv.slice(2)[0];
    if (fileName) {
        initParams.mdFileName = fileName;
        readFile(initParams.mdFileName, { encoding: 'utf-8' }, (error, document) => {
            if (error) {
                throw new Error('Ошибка открытия MD-файла ' + initParams.mdFileName + '[' + error + ']');
            }

            mkdir(initParams.mdTempDirectory, { recursive: true }, (errCreateLinkDir) => {
                parseMdLinks(document, (error, mdlinks) => {
                    prepareLinks(mdlinks, (preapreError, complete) => {
                        fillMdLinks(complete)
                    })
                });
            });
        });
    }
}

readMd();
