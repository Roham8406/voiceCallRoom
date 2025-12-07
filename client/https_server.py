import http.server
import ssl
import pathlib

PORT = 8000
DIRECTORY = pathlib.Path(".")  # your HTML folder

class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIRECTORY, **kwargs)

# Create HTTP server
httpd = http.server.HTTPServer(("0.0.0.0", PORT), Handler)

# Create SSL context
context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
context.load_cert_chain(certfile="cert.pem", keyfile="key.pem")

# Wrap the server socket with SSL
httpd.socket = context.wrap_socket(httpd.socket, server_side=True)

print(f"Serving HTTPS on https://0.0.0.0:{PORT}")
httpd.serve_forever()

