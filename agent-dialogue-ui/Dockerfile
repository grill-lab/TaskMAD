# pull official base image
FROM node:14-slim as builder
# set working directory
WORKDIR /app

# allow for setting the location of the recipes JSON file that the RecipeService needs
# and similar for the default backend endpoint URL. Since this is a React app the env var
# names need to be prefixed with "REACT_APP_" for them to be visible (see the docs at
# https://create-react-app.dev/docs/adding-custom-environment-variables)
ARG recipe_url
ENV REACT_APP_RECIPE_URL=${recipe_url:-"http://gem.cs.ru.nl/grad-pkg/radboud_taskmad.json"}
ARG data_url
ENV REACT_APP_DATA_URL=${data_url:-"http://gem.cs.ru.nl/grad-pkg/radboud_taskmad_data.json"}
ARG backend_url
ENV REACT_APP_BACKEND_URL=${backend_url:-"https://backend-server.online"}

# add `/app/node_modules/.bin` to $PATH
ENV PATH /app/node_modules/.bin:$PATH
# install app dependencies
COPY package.json ./
RUN npm install
# add app
COPY . ./
# start app
RUN npm run build

FROM nginx:latest
COPY --from=builder /app/dist/chat /usr/share/nginx/html
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
