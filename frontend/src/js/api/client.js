const BASE_URL = '/api';

async function request (path, options = {})
{

    // merge header with any customer header passed.
    const headers ={
        'Content-Type' : 'application/json',
        ...options.headers,
    };

    //combine base url with path to create full url.

    const url = `${BASE_URL}${path}`;

    //configure the fetch option
    const config = {
        ...options,
        headers,
        credentials: 'same-origin',
    };
    
    // sends the request and waits for the response to be returned.
    const response = await fetch(url, config);

    // recieve the response and parse it as json. If the response is not valid 
    // json, return an empty object.

    const data = await response.json().catch(() => ({}));

    if(!response.ok) {
        const errorMessage = data.message || "An unexpected error occurred";
        throw new Error(errorMessage);
    }
    return data;
}

export const client = {
    async get(path)
    {
        return request(path,{ method: 'GET'});
    },

    async post(path, body)
    {
        return request(path, { method: 'POST', body: JSON.stringify(body)});
    }
}