./build_images_core.sh ./deploy_config build
docker run --rm --name envoy -p 5000:10000 --network host envoy:latest
