SRC_FILE = '../src/main/java/com/cmlteam/serv/HttpHandlerFavicon.java';

const fs = require('fs');

const fileBytes = fs.readFileSync('favicon.ico');

// console.info(fileBytes);

const res = ['private final byte[] faviconBytes = new byte[] {'];

for (let i = 0; i < fileBytes.length; i++) {
    const byte = fileBytes[i];
    const javaByte = (byte >= 128 ? '(byte) ' : '') + '0x' + byte.toString(16);
    // console.info(byte, javaByte);
    res.push(javaByte);
    res.push(', ');
}

res.length--; // remove trailing comma

res.push('};');


const faviconBytesDeclaration = res.join('');
console.info(faviconBytesDeclaration);

const src = fs.readFileSync(SRC_FILE, 'utf8');

const resSrc = src.replace(/\/\/ favicon start.+\/\/ favicon end/s, `// favicon start
  ${faviconBytesDeclaration}
  // favicon end`);

// console.info(resSrc);

fs.writeFileSync(SRC_FILE, resSrc, 'utf8');
