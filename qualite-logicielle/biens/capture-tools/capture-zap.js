const path = require('path');
const puppeteer = require(path.join(__dirname, 'node_modules/puppeteer-core'));

const CHROME_PATH = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const ROOT_DIR = path.resolve(__dirname, '../../../');
const CAPTURES_DIR = path.resolve(ROOT_DIR, 'docs/qualite/biens/captures');
const ZAP_DIR = path.resolve(ROOT_DIR, 'docs/qualite/biens/zap');

async function run() {
  const browser = await puppeteer.launch({
    executablePath: CHROME_PATH,
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--window-size=1366,950'],
    defaultViewport: { width: 1366, height: 950 }
  });

  const page = await browser.newPage();

  const reports = [
    {
      file: path.join(ZAP_DIR, 'zap-frontend-report.html'),
      out: path.join(CAPTURES_DIR, '25_zap_frontend.png'),
      name: 'Rapport DAST OWASP ZAP Baseline Frontend'
    },
    {
      file: path.join(ZAP_DIR, 'zap-backend-report.html'),
      out: path.join(CAPTURES_DIR, '26_zap_api.png'),
      name: 'Rapport DAST OWASP ZAP API Scan Backend'
    }
  ];

  for (const r of reports) {
    console.log(`Chargement de ${r.file}...`);
    await page.goto(`file://${r.file.replace(/\\/g, '/')}`, { waitUntil: 'networkidle2' });
    await new Promise(res => setTimeout(res, 1000));
    await page.screenshot({ path: r.out, fullPage: false });
    console.log(`[OK] Capture générée : ${r.out}`);
  }

  await browser.close();
}

run().catch(console.error);
