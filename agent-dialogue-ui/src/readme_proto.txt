How to generate protobuf files:

protoc -I../../agent-dialogue-core/src/main/proto/ \
	--js_out=import_style=commonjs:generated \
	--grpc-web_out=import_style=typescript,mode=grpcwebtext:generated \
	service.proto client.proto

If the compiler complains, you need to modify the generated .js files by hand:

If you see "'proto' is not defined"

after
  // GENERATED CODE -- DO NOT EDIT!
add
  /* eslint-disable */


If you see "Cannot compile namespaces when the '--isolatedModules' flag is provided"

after
  // GENERATED CODE -- DO NOT EDIT!
add
  // @ts-ignore

