@echo off
echo ========================================
echo Test de connexion Supabase
echo ========================================

echo.
echo 1. Test de la table surveillant...
curl -X GET "https://sidshqdnmtccxgfzzrve.supabase.co/rest/v1/surveillant" ^
  -H "apikey: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNpZHNocWRubXRjY3hnZnp6cnZlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTc1Nzc1ODQsImV4cCI6MjA3MzE1MzU4NH0.vmAZV5pR_p4qun-qgDLQevNdQxmc7zOdamz-f0zFvVc" ^
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNpZHNocWRubXRjY3hnZnp6cnZlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTc1Nzc1ODQsImV4cCI6MjA3MzE1MzU4NH0.vmAZV5pR_p4qun-qgDLQevNdQxmc7zOdamz-f0zFvVc"

echo.
echo.
echo 2. Test de la table pointage...
curl -X GET "https://sidshqdnmtccxgfzzrve.supabase.co/rest/v1/pointage" ^
  -H "apikey: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNpZHNocWRubXRjY3hnZnp6cnZlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTc1Nzc1ODQsImV4cCI6MjA3MzE1MzU4NH0.vmAZV5pR_p4qun-qgDLQevNdQxmc7zOdamz-f0zFvVc" ^
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNpZHNocWRubXRjY3hnZnp6cnZlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTc1Nzc1ODQsImV4cCI6MjA3MzE1MzU4NH0.vmAZV5pR_p4qun-qgDLQevNdQxmc7zOdamz-f0zFvVc"

echo.
echo.
echo 3. Test de la table examen...
curl -X GET "https://sidshqdnmtccxgfzzrve.supabase.co/rest/v1/examen" ^
  -H "apikey: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNpZHNocWRubXRjY3hnZnp6cnZlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTc1Nzc1ODQsImV4cCI6MjA3MzE1MzU4NH0.vmAZV5pR_p4qun-qgDLQevNdQxmc7zOdamz-f0zFvVc" ^
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNpZHNocWRubXRjY3hnZnp6cnZlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTc1Nzc1ODQsImV4cCI6MjA3MzE1MzU4NH0.vmAZV5pR_p4qun-qgDLQevNdQxmc7zOdamz-f0zFvVc"

echo.
echo ========================================
echo Test terminé
echo ========================================
pause
