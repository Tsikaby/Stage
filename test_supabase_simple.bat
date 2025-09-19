@echo off
echo ========================================
echo Test de connexion Supabase (Version Simple)
echo ========================================

echo.
echo Test de ping vers Supabase...
ping -n 1 sidshqdnmtccxgfzzrve.supabase.co

echo.
echo Test de resolution DNS...
nslookup sidshqdnmtccxgfzzrve.supabase.co

echo.
echo ========================================
echo Si le ping fonctionne, le probleme vient de curl
echo Utilisez Android Studio pour tester l'application
echo ========================================
pause

