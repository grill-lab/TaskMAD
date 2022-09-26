# pull official base image
FROM node:14-slim as builder
# set working directory
WORKDIR /app
# add `/app/node_modules/.bin` to $PATH
ENV PATH /app/node_modules/.bin:$PATH
# install app dependencies
COPY package.json ./
RUN npm install
# add app
COPY . ./
# start app
RUN chmod +x /app/node_modules/.bin/react-scripts
RUN npm run build

FROM nginx:latest
COPY --from=builder /app/dist/chat /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
