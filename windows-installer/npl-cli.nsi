!include LogicLib.nsh

!ifndef VERSION
  !define VERSION "dev"
!endif

!define APP_NAME "NPL command line interface"
!define APP_DIR "Npl-cli"
!define APP_BUILD_EXE "npl-cli-windows-x86_64-${VERSION}.exe"
!define APP_TARGET_EXE "npl.exe"
!define APP_INSTALLER "npl-cli-installer.exe"

!define REG_ROOT "HKLM"
!define REG_KEY "SOFTWARE\NoumenaDigital\${APP_DIR}"
!define REG_ENV_KEY "SYSTEM\\CurrentControlSet\\Control\\Session Manager\\"
!define REG_VAL "InstallPath"

!define BUILD_DIR "build"
!define DIST_DIR "dist"

!system 'mkdir dist 2>nul'
!system 'mkdir build 2>nul'

Name "${APP_NAME}"
OutFile "${DIST_DIR}\${APP_INSTALLER}"
LicenseData "build\LICENSE.md"
InstallDir "$PROGRAMFILES\NoumenaDigital"
InstallDirRegKey ${REG_ROOT} "${REG_KEY}" "${REG_VAL}"
RequestExecutionLevel admin

Page license
Page directory
PageEx InstFiles
       Caption ': NPL command line interface Installation'
       CompletedText 'NPL command line interface Installation Completed'
PageExEnd

Section "Install ${APP_NAME}" SecInstall
    SetOutPath "$INSTDIR"

    ;Rename target
    File /oname=${APP_TARGET_EXE} "${BUILD_DIR}\${APP_BUILD_EXE}"

    ;Record installation directory in the windows registry
    WriteRegStr ${REG_ROOT} "${REG_KEY}" "${REG_VAL}" "$INSTDIR"

    ; Register uninstaller in Windows Add/Remove Programs
    WriteRegStr ${REG_ROOT} "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_DIR}" "DisplayName" "${APP_NAME}"
    WriteRegStr ${REG_ROOT} "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_DIR}" "UninstallString" "$INSTDIR\Uninstall.exe"
    WriteRegStr ${REG_ROOT} "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_DIR}" "InstallLocation" "$INSTDIR"
    WriteRegStr ${REG_ROOT} "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_DIR}" "DisplayVersion" "${VERSION}"
    WriteRegStr ${REG_ROOT} "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_DIR}" "Publisher" "Noumena Digital AG"
    WriteRegDWORD ${REG_ROOT} "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_DIR}" "NoModify" 1
    WriteRegDWORD ${REG_ROOT} "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_DIR}" "NoRepair" 1

    ; Add installation directory to the system PATH
    EnVar::SetHKLM
    EnVar::AddValue "PATH" "$INSTDIR"
    Pop $0

    ${If} "$0" == "0"
        DetailPrint "Successfully added $INSTDIR to PATH"
    ${Else}
        DetailPrint "Failed to add $INSTDIR to PATH"
    ${EndIf}

    ; Register uninstaller
    WriteUninstaller "$INSTDIR\Uninstall.exe"
SectionEnd

; Configure uninstaller
Section "Uninstall" SecUninstall
    DetailPrint "Uninstalling from directory: $INSTDIR"

    ; Remove executable
    Delete "$INSTDIR\${APP_TARGET_EXE}"

    ; remove Uninstaller
    Delete "$INSTDIR\Uninstall.exe"

    ; Remove registry entries (only if it is the only one)
    DeleteRegKey /ifempty ${REG_ROOT} "${REG_KEY}"

    DeleteRegKey ${REG_ROOT} "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_DIR}"

    ; Remove Installation dir from PATH
    EnVar::SetHKLM
    EnVar::DeleteValue "PATH" "$INSTDIR"
    Pop $0

    ${If} "$0" == "0"
        DetailPrint "Successfully removed $INSTDIR from PATH"
    ${Else}
        DetailPrint "Failed to remove $INSTDIR from PATH"
    ${EndIf}

    ; remove installation dir
    RMDir /r "$INSTDIR"
SectionEnd
