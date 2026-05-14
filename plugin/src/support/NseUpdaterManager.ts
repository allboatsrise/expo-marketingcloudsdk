import {FileManager} from './FileManager';
interface Params {
  iosPath: string;
  targetName: string;
  plistFileName: string;
}
export default class NseUpdaterManager {
  private nsePath = '';
  private plistFileName = '';
  constructor({iosPath, targetName, plistFileName}: Params) {
    this.nsePath = `${iosPath}/${targetName}`;
    this.plistFileName = plistFileName;
  }

  async updateNSEBundleVersion(version: string): Promise<void> {
    const plistFilePath = `${this.nsePath}/${this.plistFileName}`;
    let plistFile = await FileManager.readFile(plistFilePath);
    plistFile = plistFile.replace(/{{BUNDLE_VERSION}}/g, version);
    await FileManager.writeFile(plistFilePath, plistFile);
  }

  async updateNSEBundleShortVersion(version: string): Promise<void> {
    const plistFilePath = `${this.nsePath}/${this.plistFileName}`;
    let plistFile = await FileManager.readFile(plistFilePath);
    plistFile = plistFile.replace(/{{BUNDLE_SHORT_VERSION}}/g, version);
    await FileManager.writeFile(plistFilePath, plistFile);
  }
}
