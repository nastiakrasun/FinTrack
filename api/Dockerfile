FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src
COPY FinTrack.Api/FinTrack.Api.csproj FinTrack.Api/
RUN dotnet restore FinTrack.Api/FinTrack.Api.csproj
COPY FinTrack.Api/ FinTrack.Api/
RUN dotnet publish FinTrack.Api/FinTrack.Api.csproj -c Release --no-restore -o /app/publish

FROM mcr.microsoft.com/dotnet/aspnet:10.0 AS runtime
WORKDIR /app
COPY --from=build /app/publish .
USER $APP_UID
EXPOSE 8080
ENTRYPOINT ["dotnet", "FinTrack.Api.dll"]
