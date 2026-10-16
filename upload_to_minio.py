import os
from minio import Minio

# 1. Connect to your MinIO instance on Render
client = Minio(
    "minio-xf6i.onrender.com",
    access_key="admin",        # change if you set a different MINIO_ROOT_USER in Render
    secret_key="admin12345",    # change if you set a different MINIO_ROOT_PASSWORD in Render
    secure=True
)

# 2. Create the two required buckets
for bucket in ["projects", "starter-project"]:
    if not client.bucket_exists(bucket):
        client.make_bucket(bucket)
        print(f"Created bucket: {bucket}")
    else:
        print(f"Bucket already exists: {bucket}")

# 3. Upload template files into starter-project/react-vite-tailwind-daisyui-starter/
starter_dir = "react-vite-tailwind-daisyui-starter"
template_name = "react-vite-tailwind-daisyui-starter"

if os.path.exists(starter_dir):
    print(f"\nUploading template files from '{starter_dir}'...")
    count = 0
    for root, _, files in os.walk(starter_dir):
        for file in files:
            local_path = os.path.join(root, file)
            rel_path = os.path.relpath(local_path, starter_dir).replace("\\", "/")
            object_name = f"{template_name}/{rel_path}"
            
            client.fput_object("starter-project", object_name, local_path)
            print(f"  Uploaded: {object_name}")
            count += 1
            
    print(f"\nSuccess! Uploaded {count} files to 'starter-project/{template_name}/'.")
else:
    print(f"Error: Could not find folder '{starter_dir}' in the current directory.")
