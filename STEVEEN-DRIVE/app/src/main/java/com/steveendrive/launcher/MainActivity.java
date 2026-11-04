
    // MAPAS: ABRIR GOOGLE MAPS PARA ELEGIR DESTINO

    private void openMaps() {

        try {

            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("geo:0,0?q=")
            );

            intent.setPackage("com.google.android.apps.maps");

            startActivity(intent);

        } catch (ActivityNotFoundException e) {

            try {

                Intent fallback = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("geo:0,0?q=")
                );

                startActivity(fallback);

            } catch (Exception ex) {

                showToast("Google Maps no está instalado");
            }

        } catch (Exception e) {

            showToast("No se pudo abrir Google Maps");
        }
    }
