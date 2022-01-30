import fs from 'fs'
import path from 'path'
import { ExportedConfig } from "@expo/config-plugins";

export function getGoogleServicesFilePath(
  config: Pick<ExportedConfig, 'android'>,
  projectRoot: string
) {
  const partialSourcePath = config.android?.googleServicesFile;
  if (!partialSourcePath) {
    throw new Error(`googleServicesFile property not set in expo config`)
  }

  const completeSourcePath = path.resolve(projectRoot, partialSourcePath);

  if (!fs.existsSync(completeSourcePath)) {
    throw new Error(`File not found at ${completeSourcePath}`)
  }

  return completeSourcePath
}